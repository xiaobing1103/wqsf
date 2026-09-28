package com.wqst.api.imports;

import com.alibaba.excel.EasyExcel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wqst.api.common.BusinessException;
import com.wqst.api.common.CryptoService;
import com.wqst.api.company.CompanyService;
import com.wqst.api.file.FileObjectEntity;
import com.wqst.api.file.FileObjectMapper;
import com.wqst.api.file.FileService;
import com.wqst.api.file.ObjectStorageService;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ImportWorker {
    private final ImportJobMapper jobs;
    private final ImportJobErrorMapper errors;
    private final FileObjectMapper files;
    private final ObjectStorageService storage;
    private final FileService fileService;
    private final CompanyService companies;
    private final CryptoService crypto;
    private final ObjectMapper json;

    public ImportWorker(ImportJobMapper jobs, ImportJobErrorMapper errors, FileObjectMapper files,
                        ObjectStorageService storage, FileService fileService, CompanyService companies,
                        CryptoService crypto, ObjectMapper json) {
        this.jobs=jobs; this.errors=errors; this.files=files; this.storage=storage;
        this.fileService=fileService; this.companies=companies; this.crypto=crypto; this.json=json;
    }

    @Async("backgroundExecutor")
    public void process(long jobId) {
        if (jobs.claim(jobId) == 0) return;
        ImportJobEntity job = jobs.selectById(jobId);
        if (job == null) return;
        try {
            FileObjectEntity source = files.selectById(job.getSourceFileId());
            byte[] bytes = storage.get(source.getObjectKey());
            List<Map<Integer,String>> rows = EasyExcel.read(new ByteArrayInputStream(bytes)).headRowNumber(0).sheet().doReadSync();
            if (rows.isEmpty()) { fail(job, "IMPORT_EMPTY", "Excel 中没有表头和数据"); return; }
            validateHeader(rows.getFirst());
            job.setTotalRows(Math.max(0, rows.size()-1));
            int success=0, skipped=0, failed=0;
            Set<String> seen = new HashSet<>();
            List<ErrorRow> errorRows = new ArrayList<>();
            for (int i=1; i<rows.size(); i++) {
                Map<Integer,String> row=rows.get(i);
                int rowNo=i+1;
                if (blank(row)) { skipped++; continue; }
                String company=value(row,0), credit=value(row,1), contact=value(row,2), phone=value(row,3);
                String province=value(row,4), city=value(row,5), legal=value(row,6), email=value(row,7), address=value(row,8);
                try {
                    validate(company,credit,phone,email);
                    String dedupe=StringUtils.hasText(credit)?credit:company+"|"+phone;
                    if(!seen.add(dedupe)){skipped++;continue;}
                    if(!Boolean.TRUE.equals(job.getDryRun())) companies.create(new CompanyService.CreateCompany(company,credit,contact,phone,legal,province,city,email,address));
                    success++;
                } catch(BusinessException ex) {
                    if("COMPANY_CREDIT_CODE_EXISTS".equals(ex.code())){skipped++;continue;}
                    failed++; record(job,rowNo,ex.code(),ex.getMessage(),company,credit,phone,errorRows);
                } catch(RuntimeException ex) {
                    failed++; record(job,rowNo,"IMPORT_ROW_INVALID","该行数据格式不正确",company,credit,phone,errorRows);
                }
            }
            job.setSuccessRows(success); job.setSkippedRows(skipped); job.setFailedRows(failed);
            if(!errorRows.isEmpty()) job.setErrorReportFileId(createReport(job,errorRows));
            job.setStatus("SUCCEEDED"); job.setCompletedAt(LocalDateTime.now()); jobs.updateById(job);
        } catch(BusinessException ex) {
            fail(job,ex.code(),ex.getMessage());
        } catch(RuntimeException ex) {
            fail(job,"IMPORT_PROCESS_FAILED","导入任务处理失败");
        }
    }

    private void validateHeader(Map<Integer,String> row){for(int i=0;i<ImportService.HEADERS.size();i++)if(!ImportService.HEADERS.get(i).equals(value(row,i)))throw new BusinessException("IMPORT_HEADER_INVALID","Excel 表头与模板不一致",HttpStatus.BAD_REQUEST);}
    private void validate(String company,String credit,String phone,String email){if(!StringUtils.hasText(company))throw bad("公司名称不能为空");if(StringUtils.hasText(credit)&&!credit.matches("^[0-9A-Z]{18}$"))throw bad("统一社会信用代码格式不正确");if(StringUtils.hasText(phone)&&!phone.matches("^[0-9+ -]{7,20}$"))throw bad("联系电话格式不正确");if(StringUtils.hasText(email)&&!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))throw bad("公司邮箱格式不正确");}
    private BusinessException bad(String message){return new BusinessException("IMPORT_ROW_INVALID",message,HttpStatus.BAD_REQUEST);}

    private void record(ImportJobEntity job,int rowNo,String code,String message,String company,String credit,String phone,List<ErrorRow> report){
        try {
            Map<String,Object> snapshot=new LinkedHashMap<>();snapshot.put("companyName",company);snapshot.put("creditCodeMask",maskCredit(credit));snapshot.put("contactPhoneMask",crypto.maskPhone(phone));
            ImportJobErrorEntity error=new ImportJobErrorEntity();error.setJobId(job.getId());error.setRowNumber(rowNo);error.setErrorCode(code);error.setErrorMessage(message);error.setRowSnapshot(json.writeValueAsString(snapshot));errors.insert(error);
            report.add(new ErrorRow(rowNo,code,message,company,maskCredit(credit),crypto.maskPhone(phone)));
        } catch(Exception ex) { throw new IllegalStateException("记录导入错误失败",ex); }
    }

    private Long createReport(ImportJobEntity job,List<ErrorRow> rows){
        try(ByteArrayOutputStream out=new ByteArrayOutputStream()){
            EasyExcel.write(out,ErrorRow.class).sheet("错误行").doWrite(rows);byte[] bytes=out.toByteArray();
            String key="import-errors/"+job.getJobNo()+"-"+UUID.randomUUID()+".xlsx";
            storage.put(key,bytes,"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            try{return fileService.saveSystemFile(key,job.getJobNo()+"_错误报告.xlsx","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet","xlsx",bytes.length,ImportService.sha(bytes),job.getOperatorId(),true).getId();}
            catch(RuntimeException ex){storage.deleteQuietly(key);throw ex;}
        } catch(Exception ex){throw new IllegalStateException("生成导入错误报告失败",ex);}
    }

    private void fail(ImportJobEntity job,String code,String message){job.setStatus("FAILED");job.setFailedRows(Math.max(1,job.getFailedRows()==null?0:job.getFailedRows()));job.setCompletedAt(LocalDateTime.now());jobs.updateById(job);ImportJobErrorEntity error=new ImportJobErrorEntity();error.setJobId(job.getId());error.setRowNumber(0);error.setErrorCode(code);error.setErrorMessage(message);error.setRowSnapshot("{}");errors.insert(error);}
    private boolean blank(Map<Integer,String> row){return row==null||row.values().stream().allMatch(v->!StringUtils.hasText(v));}
    private String value(Map<Integer,String> row,int index){Object value=row==null?null:row.get(index);return value==null?null:value.toString().trim();}
    private String maskCredit(String v){return !StringUtils.hasText(v)||v.length()<6?v:v.substring(0,4)+"************"+v.substring(v.length()-2);}

    @com.alibaba.excel.annotation.ExcelIgnoreUnannotated
    public static class ErrorRow {
        @com.alibaba.excel.annotation.ExcelProperty("行号") private Integer rowNumber;
        @com.alibaba.excel.annotation.ExcelProperty("错误码") private String errorCode;
        @com.alibaba.excel.annotation.ExcelProperty("错误说明") private String errorMessage;
        @com.alibaba.excel.annotation.ExcelProperty("公司名称") private String companyName;
        @com.alibaba.excel.annotation.ExcelProperty("信用代码（脱敏）") private String creditCodeMask;
        @com.alibaba.excel.annotation.ExcelProperty("联系电话（脱敏）") private String contactPhoneMask;
        public ErrorRow(){}
        public ErrorRow(Integer rowNumber,String errorCode,String errorMessage,String companyName,String creditCodeMask,String contactPhoneMask){this.rowNumber=rowNumber;this.errorCode=errorCode;this.errorMessage=errorMessage;this.companyName=companyName;this.creditCodeMask=creditCodeMask;this.contactPhoneMask=contactPhoneMask;}
        public Integer getRowNumber(){return rowNumber;} public String getErrorCode(){return errorCode;} public String getErrorMessage(){return errorMessage;} public String getCompanyName(){return companyName;} public String getCreditCodeMask(){return creditCodeMask;} public String getContactPhoneMask(){return contactPhoneMask;}
    }
}
