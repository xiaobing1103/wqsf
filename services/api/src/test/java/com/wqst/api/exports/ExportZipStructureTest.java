package com.wqst.api.exports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wqst.api.caseorder.CaseService;
import com.wqst.api.config.AppProperties;
import com.wqst.api.file.FileService;
import com.wqst.api.file.ObjectStorageService;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ExportZipStructureTest {
    @Test void createsElevenDirectoriesAndReviewWorkbook(){ExportJobMapper jobs=org.mockito.Mockito.mock(ExportJobMapper.class);CaseService cases=org.mockito.Mockito.mock(CaseService.class);FileService files=org.mockito.Mockito.mock(FileService.class);ObjectStorageService storage=org.mockito.Mockito.mock(ObjectStorageService.class);AppProperties props=new AppProperties(new AppProperties.Security("x"),new AppProperties.Bootstrap("",""),new AppProperties.Wechat("",""),new AppProperties.S3("http://x","a","b","c","r"),new AppProperties.Export(30,1),new AppProperties.Auth(true));ExportWorker worker=new ExportWorker(jobs,cases,files,storage,new ObjectMapper(),props);List<CaseService.MaterialView> materials=new ArrayList<>();for(int i=1;i<=11;i++)materials.add(new CaseService.MaterialView((long)i,"M"+i,"资料"+i,i==11?"URL":"FILE",true,false,i==11?"https://example.invalid/report":null,"SUBMITTED","PENDING",null,null,0));CaseService.CaseView view=new CaseService.CaseView(1L,"BD001",1L,1L,"示例企业","TRADE_INCREMENT","PENDING_REVIEW","2026-09",null,2L,null,null,new CaseService.Progress(11,11,0),materials,null,null,null,null,null,0);when(cases.adminDetail(1L)).thenReturn(view);when(files.readyByCase(1L)).thenReturn(List.of());byte[] zip=ReflectionTestUtils.invokeMethod(worker,"materialZip",1L,true);Set<String> names=new HashSet<>();try(ZipInputStream in=new ZipInputStream(new ByteArrayInputStream(zip))){ZipEntry entry;while((entry=in.getNextEntry())!=null)names.add(entry.getName());}catch(Exception ex){throw new AssertionError(ex);}assertThat(names.stream().filter(n->n.matches("\\d{2}_资料\\d+/"))).hasSize(11);assertThat(names).contains("审核表.xlsx");assertThat(names.stream().filter(n->n.endsWith("水母报告链接.txt"))).hasSize(1);}
}
