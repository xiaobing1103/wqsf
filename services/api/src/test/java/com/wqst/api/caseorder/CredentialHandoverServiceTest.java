package com.wqst.api.caseorder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wqst.api.audit.AuditService;
import com.wqst.api.caseorder.entity.CredentialHandoverEntity;
import com.wqst.api.caseorder.entity.ServiceCaseEntity;
import com.wqst.api.caseorder.mapper.CredentialHandoverMapper;
import com.wqst.api.common.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CredentialHandoverServiceTest {
    private final CredentialHandoverMapper handovers = mock(CredentialHandoverMapper.class);
    private final CaseService cases = mock(CaseService.class);
    private final AuditService audit = mock(AuditService.class);
    private CredentialHandoverService service;

    @BeforeEach
    void setup() {
        service = new CredentialHandoverService(handovers, cases, audit);
        ServiceCaseEntity trade = new ServiceCaseEntity();
        trade.setId(1L);
        trade.setServiceType("TRADE_INCREMENT");
        when(cases.require(1L)).thenReturn(trade);
    }

    @Test
    void returnsPendingViewBeforeFirstSave() {
        CredentialHandoverService.CredentialHandoverView result = service.get(1L);
        assertThat(result.id()).isNull();
        assertThat(result.version()).isZero();
        assertThat(result.onlineBankingStatus()).isEqualTo("PENDING");
    }

    @Test
    void savesOnlyHandoverStatusesAndAuditMetadata() {
        CredentialHandoverEntity stored = new CredentialHandoverEntity();
        stored.setId(8L); stored.setCaseId(1L); stored.setVersion(0);
        stored.setOnlineBankingStatus("HANDED_OVER"); stored.setUShieldStatus("VERIFIED");
        stored.setTransactionAuthorizationStatus("NOT_REQUIRED"); stored.setHandledBy(9L);
        when(handovers.selectOne(any())).thenReturn(null, stored);
        CredentialHandoverService.CredentialHandoverView result = service.save(1L,
                new CredentialHandoverService.CredentialHandoverCommand("HANDED_OVER", "VERIFIED", "NOT_REQUIRED", 0), 9L);
        assertThat(result.handledBy()).isEqualTo(9L);
        verify(handovers).insert(any(CredentialHandoverEntity.class));
        verify(audit).operation(9L, "CREDENTIAL_HANDOVER_UPDATE", "CASE", 1L, "更新线下凭证交接状态（未保存密码）");
    }

    @Test
    void rejectsInvalidStatusAndNonTradeCase() {
        assertThatThrownBy(() -> service.save(1L,
                new CredentialHandoverService.CredentialHandoverCommand("SECRET_VALUE", "PENDING", "PENDING", 0), 9L))
                .isInstanceOf(BusinessException.class).extracting("code").isEqualTo("CREDENTIAL_HANDOVER_STATUS_INVALID");
        ServiceCaseEntity ip = new ServiceCaseEntity(); ip.setId(2L); ip.setServiceType("IP");
        when(cases.require(2L)).thenReturn(ip);
        assertThatThrownBy(() -> service.get(2L)).isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("CREDENTIAL_HANDOVER_NOT_APPLICABLE");
    }

    @Test
    void rejectsStaleVersion() {
        CredentialHandoverEntity stored = new CredentialHandoverEntity();
        stored.setId(8L); stored.setCaseId(1L); stored.setVersion(2);
        when(handovers.selectOne(any())).thenReturn(stored);
        assertThatThrownBy(() -> service.save(1L,
                new CredentialHandoverService.CredentialHandoverCommand("PENDING", "PENDING", "PENDING", 1), 9L))
                .isInstanceOf(BusinessException.class).extracting("code").isEqualTo("OPTIMISTIC_LOCK_CONFLICT");
    }
}
