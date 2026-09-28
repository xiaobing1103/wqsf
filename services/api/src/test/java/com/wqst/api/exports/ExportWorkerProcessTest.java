package com.wqst.api.exports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wqst.api.caseorder.CaseService;
import com.wqst.api.config.AppProperties;
import com.wqst.api.file.FileService;
import com.wqst.api.file.ObjectStorageService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExportWorkerProcessTest {
    ExportJobMapper jobs=mock(ExportJobMapper.class);CaseService cases=mock(CaseService.class);FileService files=mock(FileService.class);ObjectStorageService storage=mock(ObjectStorageService.class);ExportWorker worker;ExportJobEntity job;
    @BeforeEach void setup(){AppProperties p=new AppProperties(new AppProperties.Security("x"),new AppProperties.Bootstrap("",""),new AppProperties.Wechat("",""),new AppProperties.S3("http://x","a","b","c","r"),new AppProperties.Export(30,1),new AppProperties.Auth(true));worker=new ExportWorker(jobs,cases,files,storage,new ObjectMapper(),p);job=new ExportJobEntity();job.setId(7L);job.setStatus("PENDING");job.setExportType("REVIEW_EXCEL");job.setRequestPayload("{\"caseIds\":[1],\"includeSensitive\":false}");job.setVersion(0);when(jobs.claim(7L)).thenReturn(1);when(jobs.selectById(7L)).thenReturn(job);when(jobs.updateById(any(ExportJobEntity.class))).thenReturn(1);CaseService.CaseView c=new CaseService.CaseView(1L,"BD1",1L,1L,"示例企业","IP","PENDING_REVIEW",null,null,2L,null,null,new CaseService.Progress(3,3,0),List.of(),null,null,null,null,null,0);when(cases.adminDetail(1L)).thenReturn(c);}
    @Test void claimsAndCompletesExcelJob(){worker.process(7);assertThat(job.getStatus()).isEqualTo("SUCCEEDED");assertThat(job.getObjectKey()).startsWith("exports/7/").endsWith(".xlsx");verify(storage).put(startsWith("exports/7/"),any(byte[].class),eq("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));}
    @Test void marksJobFailedWithoutLeakingException(){job.setRequestPayload("not-json");worker.process(7);assertThat(job.getStatus()).isEqualTo("FAILED");assertThat(job.getErrorCode()).isEqualTo("EXPORT_GENERATION_FAILED");assertThat(job.getErrorMessage()).doesNotContain("Json");}
    @Test void skipsAlreadyClaimedJob(){when(jobs.claim(7)).thenReturn(0);worker.process(7);verify(jobs,never()).selectById(anyLong());}
}
