package com.wqst.api.file;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wqst.api.audit.AuditService;
import com.wqst.api.caseorder.CaseService;
import com.wqst.api.caseorder.mapper.MaterialSubmissionMapper;
import com.wqst.api.catalog.mapper.MaterialTemplateItemMapper;
import com.wqst.api.common.BusinessException;
import com.wqst.api.common.SecuritySupport;
import com.wqst.api.company.CompanyService;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FileServiceSecurityTest {
    @Mock FileObjectMapper files;
    @Mock MaterialTemplateItemMapper templateItems;
    @Mock MaterialSubmissionMapper materials;
    @Mock ObjectStorageService storage;
    @Mock FileInspector inspector;
    @Mock CaseService cases;
    @Mock CompanyService companies;
    @Mock SecuritySupport security;
    @Mock AuditService audit;
    private FileService service;

    @BeforeEach
    void setUp() {
        service = new FileService(files, templateItems, materials, storage, inspector, cases, companies, security, audit);
    }

    @Test
    void adminPreviewAlwaysRequiresCaseViewAndSensitivePermissionWhenNeeded() {
        FileObjectEntity file = readyFile(10L, true);
        when(files.selectById(1L)).thenReturn(file);
        when(security.isAdmin()).thenReturn(true);
        when(security.adminId()).thenReturn(7L);
        when(storage.presign(any(), any(Duration.class))).thenReturn("http://example.invalid/preview");

        service.preview(1L);

        verify(security).permission("case:view");
        verify(security).permission("file:sensitive:view");
        verify(audit).file(7L, 1L, 10L, "PREVIEW", "SUCCESS");
    }

    @Test
    void clientCannotDeleteSystemFile() {
        when(files.selectById(2L)).thenReturn(readyFile(null, false));

        assertThatThrownBy(() -> service.delete(2L, 8L))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("FILE_DELETE_DENIED");
    }

    private FileObjectEntity readyFile(Long caseId, boolean sensitive) {
        FileObjectEntity file = new FileObjectEntity();
        file.setId(caseId == null ? 2L : 1L);
        file.setCaseId(caseId);
        file.setObjectKey("cases/test.jpg");
        file.setStatus("READY");
        file.setIsSensitive(sensitive);
        return file;
    }
}
