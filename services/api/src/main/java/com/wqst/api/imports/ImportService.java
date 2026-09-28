package com.wqst.api.imports;

import com.alibaba.excel.EasyExcel;
import com.fasterxml.jackson.core.type.TypeReference;
import com.wqst.api.audit.AuditService;
import com.wqst.api.common.BusinessException;
import com.wqst.api.common.IdempotencyService;
import com.wqst.api.file.FileInspector;
import com.wqst.api.file.FileObjectEntity;
import com.wqst.api.file.FileObjectMapper;
import com.wqst.api.file.FileService;
import com.wqst.api.file.ObjectStorageService;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImportService {
    public static final List<String> HEADERS=List.of("公司名称","统一社会信用代码","联系人","联系电话","省","市","法人姓名","公司邮箱","详细地址");
    private final ImportJobMapper jobs;private final ImportJobErrorMapper errors;private final FileObjectMapper files;private final FileService fileService;
    private final ObjectStorageService storage;private final FileInspector inspector;private final IdempotencyService idempotency;private final ImportWorker worker;private final AuditService audit;
    public ImportService(ImportJobMapper jobs,ImportJobErrorMapper errors,FileObjectMapper files,FileService fileService,ObjectStorageService storage,FileInspector inspector,IdempotencyService idempotency,ImportWorker worker,AuditService audit){this.jobs=jobs;this.errors=errors;this.files=files;this.fileService=fileService;this.storage=storage;this.inspector=inspector;this.idempotency=idempotency;this.worker=worker;this.audit=audit;}

    @Transactional
    public TemplateDownload template(long operatorId){byte[] bytes=templateBytes();String key="import-templates/company-import-"+UUID.randomUUID()+".xlsx";storage.put(key,bytes,"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");storage.deleteOnRollback(key);fileService.saveSystemFile(key,"客户批量导入模板.xlsx","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet","xlsx",bytes.length,sha(bytes),operatorId,false);return new TemplateDownload(storage.presign(key,Duration.ofMinutes(30)),OffsetDateTime.now().plusMinutes(30));}

    public JobView create(MultipartFile upload,boolean dryRun,long operatorId,String key){return idempotency.execute(Long.toString(operatorId),"company-import",key,new TypeReference<JobView>(){},()->createTransaction(upload,dryRun,operatorId));}
    protected JobView createTransaction(MultipartFile upload,boolean dryRun,long operatorId){Path staged=null;String objectKey=null;try{staged=Files.createTempFile("wqst-import-",".xlsx");upload.transferTo(staged);byte[] header;try(var in=Files.newInputStream(staged)){header=in.readNBytes(16);}inspector.validate(upload.getOriginalFilename(),upload.getContentType(),Files.size(staged),header,Set.of("xlsx"),20L*1024*1024);inspector.validateXlsx(staged);String uuid=UUID.randomUUID().toString();objectKey="imports/"+uuid+".xlsx";storage.put(objectKey,staged,"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");storage.deleteOnRollback(objectKey);byte[] all=Files.readAllBytes(staged);FileObjectEntity source=fileService.saveSystemFile(objectKey,upload.getOriginalFilename(),"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet","xlsx",all.length,sha(all),operatorId,true);ImportJobEntity job=new ImportJobEntity();job.setJobNo(nextNo());job.setSourceFileId(source.getId());job.setTemplateCode("COMPANY_IMPORT_V1");job.setStatus("PENDING");job.setDryRun(dryRun);job.setTotalRows(0);job.setSuccessRows(0);job.setSkippedRows(0);job.setFailedRows(0);job.setOperatorId(operatorId);jobs.insert(job);afterCommit(()->worker.process(job.getId()));audit.operation(operatorId,"COMPANY_IMPORT_CREATE","IMPORT",job.getId(),"创建客户 Excel 导入任务");return view(job);}catch(BusinessException ex){if(objectKey!=null)storage.deleteQuietly(objectKey);throw ex;}catch(Exception ex){if(objectKey!=null)storage.deleteQuietly(objectKey);throw new BusinessException("IMPORT_UPLOAD_FAILED","导入文件接收失败",HttpStatus.INTERNAL_SERVER_ERROR);}finally{if(staged!=null)try{Files.deleteIfExists(staged);}catch(Exception ignored){}}}

    public JobView get(long id,long operatorId){ImportJobEntity job=require(id);if(!job.getOperatorId().equals(operatorId))throw new BusinessException("IMPORT_SCOPE_DENIED","无权访问该导入任务",HttpStatus.FORBIDDEN);return view(job);}
    public TemplateDownload errorReport(long id,long operatorId){ImportJobEntity job=require(id);if(!job.getOperatorId().equals(operatorId))throw new BusinessException("IMPORT_SCOPE_DENIED","无权访问该导入任务",HttpStatus.FORBIDDEN);if(job.getErrorReportFileId()==null)throw new BusinessException("IMPORT_ERROR_REPORT_NOT_READY","该任务没有错误报告",HttpStatus.CONFLICT);FileObjectEntity f=files.selectById(job.getErrorReportFileId());return new TemplateDownload(storage.presign(f.getObjectKey(),Duration.ofMinutes(30)),OffsetDateTime.now().plusMinutes(30));}
    public ImportJobEntity require(long id){ImportJobEntity job=jobs.selectById(id);if(job==null)throw new BusinessException("IMPORT_JOB_NOT_FOUND","导入任务不存在",HttpStatus.NOT_FOUND);return job;}
    private byte[] templateBytes(){try(ByteArrayOutputStream out=new ByteArrayOutputStream()){List<List<String>> head=HEADERS.stream().map(List::of).toList();EasyExcel.write(out).head(head).sheet("客户导入").doWrite(List.of());return out.toByteArray();}catch(Exception ex){throw new IllegalStateException("生成导入模板失败",ex);}}
    private String nextNo(){return "DR"+LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))+UUID.randomUUID().toString().substring(0,4).toUpperCase();}
    static String sha(byte[] value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));}catch(Exception ex){throw new IllegalStateException(ex);}}
    static JobView view(ImportJobEntity j){return new JobView(j.getId(),j.getJobNo(),j.getStatus(),Boolean.TRUE.equals(j.getDryRun()),n(j.getTotalRows()),n(j.getSuccessRows()),n(j.getSkippedRows()),n(j.getFailedRows()),j.getErrorReportFileId(),j.getCreatedAt(),j.getCompletedAt());}
    private static int n(Integer v){return v==null?0:v;}
    private void afterCommit(Runnable action){if(!TransactionSynchronizationManager.isSynchronizationActive()){action.run();return;}TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCommit(){action.run();}});}
    public record JobView(Long id,String jobNo,String status,boolean dryRun,int totalRows,int successRows,int skippedRows,int failedRows,Long errorReportFileId,LocalDateTime createdAt,LocalDateTime completedAt){}
    public record TemplateDownload(String downloadUrl,OffsetDateTime expiresAt){}
}
