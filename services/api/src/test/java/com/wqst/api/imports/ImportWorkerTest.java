package com.wqst.api.imports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.alibaba.excel.EasyExcel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wqst.api.common.CryptoService;
import com.wqst.api.common.BusinessException;
import org.springframework.http.HttpStatus;
import com.wqst.api.company.CompanyService;
import com.wqst.api.file.FileObjectEntity;
import com.wqst.api.file.FileObjectMapper;
import com.wqst.api.file.FileService;
import com.wqst.api.file.ObjectStorageService;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ImportWorkerTest {
    ImportJobMapper jobs=mock(ImportJobMapper.class);ImportJobErrorMapper errors=mock(ImportJobErrorMapper.class);FileObjectMapper files=mock(FileObjectMapper.class);ObjectStorageService storage=mock(ObjectStorageService.class);FileService fileService=mock(FileService.class);CompanyService companies=mock(CompanyService.class);CryptoService crypto=mock(CryptoService.class);ImportWorker worker;ImportJobEntity job;
    @BeforeEach void setup(){worker=new ImportWorker(jobs,errors,files,storage,fileService,companies,crypto,new ObjectMapper());job=new ImportJobEntity();job.setId(8L);job.setSourceFileId(9L);job.setStatus("PENDING");job.setDryRun(true);job.setFailedRows(0);job.setOperatorId(2L);job.setJobNo("DR001");when(jobs.claim(8L)).thenReturn(1);when(jobs.selectById(8L)).thenReturn(job);FileObjectEntity source=new FileObjectEntity();source.setObjectKey("imports/source.xlsx");when(files.selectById(9L)).thenReturn(source);when(crypto.maskPhone(any())).thenAnswer(i->"***"+i.getArgument(0));FileObjectEntity report=new FileObjectEntity();report.setId(88L);when(fileService.saveSystemFile(anyString(),anyString(),anyString(),anyString(),anyLong(),anyString(),anyLong(),eq(true))).thenReturn(report);}
    @Test void countsSuccessDuplicateAndInvalidRows(){List<List<String>> rows=new ArrayList<>();rows.add(List.of("甲公司","91430100123456789X","张三","13800000000","湖南省","衡阳市","张法人","a@example.com","地址1"));rows.add(List.of("甲公司","91430100123456789X","张三","13800000000","湖南省","衡阳市","张法人","a@example.com","地址1"));rows.add(List.of("乙公司","91430100123456788X","李四","BAD","湖南省","衡阳市","李法人","b@example.com","地址2"));when(storage.get("imports/source.xlsx")).thenReturn(workbook(ImportService.HEADERS,rows));worker.process(8);assertThat(job.getStatus()).isEqualTo("SUCCEEDED");assertThat(job.getTotalRows()).isEqualTo(3);assertThat(job.getSuccessRows()).isEqualTo(1);assertThat(job.getSkippedRows()).isEqualTo(1);assertThat(job.getFailedRows()).isEqualTo(1);assertThat(job.getErrorReportFileId()).isEqualTo(88L);verify(errors).insert(argThat((ImportJobErrorEntity e)->e.getRowNumber()==4&&"IMPORT_ROW_INVALID".equals(e.getErrorCode())&&!e.getRowSnapshot().contains("13800000000")));}
    @Test void rejectsChangedHeader(){List<String> bad=new ArrayList<>(ImportService.HEADERS);bad.set(0,"企业");when(storage.get("imports/source.xlsx")).thenReturn(workbook(bad,List.of()));worker.process(8);assertThat(job.getStatus()).isEqualTo("FAILED");verify(errors).insert(argThat((ImportJobErrorEntity e)->e.getRowNumber()==0&&"IMPORT_HEADER_INVALID".equals(e.getErrorCode())));}
    @Test void skipsDatabaseDuplicateOutsideDryRun(){job.setDryRun(false);List<List<String>> rows=List.of(List.of("甲公司","91430100123456789X","张三","13800000000","湖南省","衡阳市","法人","a@example.com","地址"));when(storage.get("imports/source.xlsx")).thenReturn(workbook(ImportService.HEADERS,rows));when(companies.create(any())).thenThrow(new BusinessException("COMPANY_CREDIT_CODE_EXISTS","重复",HttpStatus.CONFLICT));worker.process(8);assertThat(job.getStatus()).isEqualTo("SUCCEEDED");assertThat(job.getSkippedRows()).isEqualTo(1);assertThat(job.getFailedRows()).isZero();}
    @Test void recordsAllValidationBranches(){List<List<String>> rows=List.of(List.of("","91430100123456789X","","13800000000","","","","",""),List.of("甲公司","BAD","","13800000000","","","","",""),List.of("乙公司","91430100123456788X","","13800000000","","","","bad-email",""));when(storage.get("imports/source.xlsx")).thenReturn(workbook(ImportService.HEADERS,rows));worker.process(8);assertThat(job.getFailedRows()).isEqualTo(3);verify(errors,times(3)).insert(any(ImportJobErrorEntity.class));}
    @Test void convertsUnexpectedRowFailureToSafeError(){job.setDryRun(false);List<List<String>> rows=List.of(List.of("甲公司","91430100123456789X","张三","13800000000","湖南省","衡阳市","法人","a@example.com","地址"));when(storage.get("imports/source.xlsx")).thenReturn(workbook(ImportService.HEADERS,rows));when(companies.create(any())).thenThrow(new IllegalStateException("sensitive internal detail"));worker.process(8);assertThat(job.getFailedRows()).isEqualTo(1);verify(errors).insert(argThat((ImportJobErrorEntity e)->"IMPORT_ROW_INVALID".equals(e.getErrorCode())&&!e.getErrorMessage().contains("sensitive")));}
    @Test void doesNothingWhenAnotherWorkerClaimedJob(){when(jobs.claim(8L)).thenReturn(0);worker.process(8);verify(jobs,never()).selectById(anyLong());}
    private byte[] workbook(List<String> headers,List<List<String>> rows){try(ByteArrayOutputStream out=new ByteArrayOutputStream()){EasyExcel.write(out).head(headers.stream().map(List::of).toList()).sheet("客户导入").doWrite(rows);return out.toByteArray();}catch(Exception ex){throw new AssertionError(ex);}}
}
