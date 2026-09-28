package com.wqst.api.exports;

import com.alibaba.excel.EasyExcel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wqst.api.caseorder.CaseService;
import com.wqst.api.common.SafeFiles;
import com.wqst.api.common.BusinessException;
import com.wqst.api.config.AppProperties;
import com.wqst.api.file.FileObjectEntity;
import com.wqst.api.file.FileService;
import com.wqst.api.file.ObjectStorageService;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ExportWorker {
    private final ExportJobMapper jobs;private final CaseService cases;private final FileService files;private final ObjectStorageService storage;
    private final ObjectMapper json;private final AppProperties props;
    public ExportWorker(ExportJobMapper jobs,CaseService cases,FileService files,ObjectStorageService storage,ObjectMapper json,AppProperties props){this.jobs=jobs;this.cases=cases;this.files=files;this.storage=storage;this.json=json;this.props=props;}
    @Async("exportExecutor")
    public void process(long jobId){if(jobs.claim(jobId)==0)return;ExportJobEntity job=jobs.selectById(jobId);String uploadedKey=null;try{ExportService.ExportPayload payload=json.readValue(job.getRequestPayload(),ExportService.ExportPayload.class);byte[] bytes;String extension;String contentType;if("REVIEW_EXCEL".equals(job.getExportType())){bytes=reviewExcel(payload.caseIds());extension="xlsx";contentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";}else{bytes=materialZip(payload.caseIds().getFirst(),payload.includeSensitive());extension="zip";contentType="application/zip";}uploadedKey="exports/"+jobId+"/"+UUID.randomUUID()+"."+extension;storage.put(uploadedKey,bytes,contentType);job=jobs.selectById(jobId);job.setObjectKey(uploadedKey);job.setStatus("SUCCEEDED");job.setExpiresAt(LocalDateTime.now().plusMinutes(props.export().linkTtlMinutes()));job.setCompletedAt(LocalDateTime.now());if(jobs.updateById(job)==0)throw new IllegalStateException("导出任务并发冲突");}catch(Exception ex){if(uploadedKey!=null)storage.deleteQuietly(uploadedKey);job=jobs.selectById(jobId);job.setStatus("FAILED");job.setErrorCode(ex instanceof BusinessException business?business.code():"EXPORT_GENERATION_FAILED");job.setErrorMessage(ex instanceof BusinessException?ex.getMessage():"导出文件生成失败");job.setCompletedAt(LocalDateTime.now());jobs.updateById(job);}}
    private byte[] reviewExcel(List<Long> ids){List<ReviewRow> rows=ids.stream().map(cases::adminDetail).map(c->new ReviewRow(c.caseNo(),c.companyName(),c.serviceType(),c.status(),c.caseMonth(),c.progress().total(),c.progress().completed(),c.progress().requiredRemaining(),c.assigneeId())).toList();try(ByteArrayOutputStream out=new ByteArrayOutputStream()){EasyExcel.write(out,ReviewRow.class).sheet("报单审核表").doWrite(rows);return out.toByteArray();}catch(Exception ex){throw new IllegalStateException(ex);}}
    private byte[] materialZip(long caseId,boolean includeSensitive){CaseService.CaseView c=cases.adminDetail(caseId);List<FileObjectEntity> all=files.readyByCase(caseId);Map<Long,List<FileObjectEntity>> byMaterial=all.stream().collect(Collectors.groupingBy(FileObjectEntity::getMaterialId));try(ByteArrayOutputStream raw=new ByteArrayOutputStream();ZipArchiveOutputStream zip=new ZipArchiveOutputStream(raw)){Set<String> names=new HashSet<>();int index=1;for(CaseService.MaterialView material:c.materials()){String dir=String.format("%02d_%s",index++,SafeFiles.fileName(material.materialName()));ZipArchiveEntry folder=new ZipArchiveEntry(dir+"/");zip.putArchiveEntry(folder);zip.closeArchiveEntry();if(material.sensitive()&&!includeSensitive)continue;int fileIndex=1;for(FileObjectEntity f:byMaterial.getOrDefault(material.id(),List.of())){String name=SafeFiles.zipEntry(dir,(fileIndex++)+"_"+f.getOriginalName());while(!names.add(name))name=SafeFiles.zipEntry(dir,UUID.randomUUID().toString().substring(0,6)+"_"+f.getOriginalName());ZipArchiveEntry entry=new ZipArchiveEntry(name);entry.setSize(f.getFileSize());zip.putArchiveEntry(entry);zip.write(storage.get(f.getObjectKey()));zip.closeArchiveEntry();}if("URL".equals(material.inputType())&&material.textValue()!=null){String name=SafeFiles.zipEntry(dir,"水母报告链接.txt");names.add(name);zip.putArchiveEntry(new ZipArchiveEntry(name));zip.write(material.textValue().getBytes(java.nio.charset.StandardCharsets.UTF_8));zip.closeArchiveEntry();}}byte[] review=reviewExcel(List.of(caseId));zip.putArchiveEntry(new ZipArchiveEntry("审核表.xlsx"));zip.write(review);zip.closeArchiveEntry();zip.finish();return raw.toByteArray();}catch(Exception ex){throw new IllegalStateException(ex);}}
    @com.alibaba.excel.annotation.ExcelIgnoreUnannotated
    public static class ReviewRow {
        @com.alibaba.excel.annotation.ExcelProperty("报单编号") private String caseNo;
        @com.alibaba.excel.annotation.ExcelProperty("公司名称") private String companyName;
        @com.alibaba.excel.annotation.ExcelProperty("服务类型") private String serviceType;
        @com.alibaba.excel.annotation.ExcelProperty("报单状态") private String status;
        @com.alibaba.excel.annotation.ExcelProperty("报单月份") private String caseMonth;
        @com.alibaba.excel.annotation.ExcelProperty("资料总数") private Long total;
        @com.alibaba.excel.annotation.ExcelProperty("已提交") private Long completed;
        @com.alibaba.excel.annotation.ExcelProperty("待补数量") private Long remaining;
        @com.alibaba.excel.annotation.ExcelProperty("负责人ID") private Long assigneeId;
        public ReviewRow(){}public ReviewRow(String caseNo,String companyName,String serviceType,String status,String caseMonth,Long total,Long completed,Long remaining,Long assigneeId){this.caseNo=caseNo;this.companyName=companyName;this.serviceType=serviceType;this.status=status;this.caseMonth=caseMonth;this.total=total;this.completed=completed;this.remaining=remaining;this.assigneeId=assigneeId;}
        public String getCaseNo(){return caseNo;}public String getCompanyName(){return companyName;}public String getServiceType(){return serviceType;}public String getStatus(){return status;}public String getCaseMonth(){return caseMonth;}public Long getTotal(){return total;}public Long getCompleted(){return completed;}public Long getRemaining(){return remaining;}public Long getAssigneeId(){return assigneeId;}
    }
}
