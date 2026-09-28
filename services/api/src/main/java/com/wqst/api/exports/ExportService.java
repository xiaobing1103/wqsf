package com.wqst.api.exports;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wqst.api.audit.AuditService;
import com.wqst.api.caseorder.CaseService;
import com.wqst.api.caseorder.entity.ServiceCaseEntity;
import com.wqst.api.common.BusinessException;
import com.wqst.api.common.IdempotencyService;
import com.wqst.api.config.AppProperties;
import com.wqst.api.file.ObjectStorageService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class ExportService {
    private final ExportJobMapper jobs;private final CaseService cases;private final IdempotencyService idempotency;private final ObjectMapper json;
    private final ExportWorker worker;private final ObjectStorageService storage;private final AppProperties props;private final AuditService audit;
    public ExportService(ExportJobMapper jobs,CaseService cases,IdempotencyService idempotency,ObjectMapper json,ExportWorker worker,ObjectStorageService storage,AppProperties props,AuditService audit){this.jobs=jobs;this.cases=cases;this.idempotency=idempotency;this.json=json;this.worker=worker;this.storage=storage;this.props=props;this.audit=audit;}

    public ExportJobView reviewExcel(List<Long> caseIds,long operatorId,String key){return idempotency.execute(Long.toString(operatorId),"review-excel",key,new TypeReference<ExportJobView>(){},()->create("REVIEW_EXCEL",caseIds,null,false,operatorId,key));}
    public ExportJobView materialZip(long caseId,boolean includeSensitive,long operatorId,String key){ServiceCaseEntity c=cases.require(caseId);if(!"TRADE_INCREMENT".equals(c.getServiceType()))throw new BusinessException("ZIP_SERVICE_TYPE_DENIED","只有贸易增量报单可以导出原始资料包",HttpStatus.BAD_REQUEST);return idempotency.execute(Long.toString(operatorId),"material-zip-"+caseId,key,new TypeReference<ExportJobView>(){},()->create("MATERIAL_ZIP",List.of(caseId),caseId,includeSensitive,operatorId,key));}
    private ExportJobView create(String type,List<Long> inputIds,Long caseId,boolean includeSensitive,long operatorId,String key){List<Long> ids=new LinkedHashSet<>(inputIds==null?List.of():inputIds).stream().toList();if(ids.isEmpty())throw new BusinessException("EXPORT_CASES_REQUIRED","请选择需要导出的报单",HttpStatus.BAD_REQUEST);ids.forEach(cases::require);try{ExportPayload payload=new ExportPayload(ids,includeSensitive);ExportJobEntity job=new ExportJobEntity();job.setCaseId(caseId);job.setRequestId(requestId(type,operatorId,key));job.setRequestPayload(json.writeValueAsString(payload));job.setExportType(type);job.setStatus("PENDING");job.setDownloadCount(0);job.setOperatorId(operatorId);jobs.insert(job);afterCommit(()->worker.process(job.getId()));audit.operation(operatorId,"EXPORT_CREATE","EXPORT",job.getId(),"创建 "+type+" 导出任务");return view(job);}catch(BusinessException ex){throw ex;}catch(Exception ex){throw new IllegalStateException("创建导出任务失败",ex);}}
    public ExportJobView get(long id,long operatorId){ExportJobEntity job=requireOwned(id,operatorId);if("SUCCEEDED".equals(job.getStatus())&&job.getExpiresAt()!=null&&job.getExpiresAt().isBefore(LocalDateTime.now())){job.setStatus("EXPIRED");jobs.updateById(job);}return view(job);}
    public DownloadView download(long id,long operatorId){ExportJobEntity job=requireOwned(id,operatorId);if(!"SUCCEEDED".equals(job.getStatus())){audit.download(operatorId,id,"DENIED");throw new BusinessException("EXPORT_NOT_READY","导出文件尚未就绪",HttpStatus.CONFLICT);}if(job.getExpiresAt()==null||job.getExpiresAt().isBefore(LocalDateTime.now())){job.setStatus("EXPIRED");jobs.updateById(job);audit.download(operatorId,id,"EXPIRED");throw new BusinessException("EXPORT_EXPIRED","导出文件已过期",HttpStatus.GONE);}long seconds=Math.max(1,Math.min(Duration.ofMinutes(props.export().linkTtlMinutes()).toSeconds(),Duration.between(LocalDateTime.now(),job.getExpiresAt()).toSeconds()));Duration ttl=Duration.ofSeconds(seconds);String url=storage.presign(job.getObjectKey(),ttl);jobs.incrementDownload(id);audit.download(operatorId,id,"SUCCESS");return new DownloadView(url,OffsetDateTime.now().plusSeconds(seconds));}
    public ExportJobEntity requireOwned(long id,long operatorId){ExportJobEntity j=jobs.selectById(id);if(j==null)throw new BusinessException("EXPORT_JOB_NOT_FOUND","导出任务不存在",HttpStatus.NOT_FOUND);if(!j.getOperatorId().equals(operatorId))throw new BusinessException("EXPORT_SCOPE_DENIED","无权访问该导出任务",HttpStatus.FORBIDDEN);return j;}
    public boolean includesSensitive(ExportJobEntity job){if(!"MATERIAL_ZIP".equals(job.getExportType())||job.getRequestPayload()==null)return false;try{return json.readValue(job.getRequestPayload(),ExportPayload.class).includeSensitive();}catch(Exception ex){throw new BusinessException("EXPORT_PAYLOAD_INVALID","导出任务数据无效",HttpStatus.CONFLICT);}}
    static ExportJobView view(ExportJobEntity j){return new ExportJobView(j.getId(),j.getCaseId(),j.getExportType(),j.getStatus(),j.getErrorCode(),j.getErrorMessage(),j.getExpiresAt(),j.getDownloadCount()==null?0:j.getDownloadCount(),j.getCreatedAt(),j.getCompletedAt());}
    private String requestId(String type,long operatorId,String key){try{return type.substring(0,Math.min(8,type.length()))+"-"+operatorId+"-"+HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8))).substring(0,32);}catch(Exception ex){throw new IllegalStateException(ex);}}
    private void afterCommit(Runnable action){if(!TransactionSynchronizationManager.isSynchronizationActive()){action.run();return;}TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCommit(){action.run();}});}
    public record ExportPayload(List<Long> caseIds,boolean includeSensitive){}
    public record ExportJobView(Long id,Long caseId,String exportType,String status,String errorCode,String errorMessage,LocalDateTime expiresAt,int downloadCount,LocalDateTime createdAt,LocalDateTime completedAt){}
    public record DownloadView(String downloadUrl,OffsetDateTime expiresAt){}
}
