package com.wqst.api.capability;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.wqst.api.caseorder.CaseService;
import com.wqst.api.common.BusinessException;
import java.util.List;
import org.junit.jupiter.api.Test;

class ClientWriteGateTest {
    @Test void checksSensitiveMaterialAndService() {
        CapabilityService flags=mock(CapabilityService.class); CaseService cases=mock(CaseService.class);
        CaseService.CaseView detail=mock(CaseService.CaseView.class); CaseService.MaterialView material=mock(CaseService.MaterialView.class);
        when(cases.clientDetail(1,9)).thenReturn(detail); when(detail.serviceType()).thenReturn("IP");
        when(flags.serviceKey("IP")).thenReturn("trademarkConsultationEnabled");
        when(detail.materials()).thenReturn(List.of(material)); when(material.id()).thenReturn(10L); when(material.sensitive()).thenReturn(true);
        new ClientWriteGate(flags,cases).write(1,9,10L,false);
        verify(flags).require("trademarkConsultationEnabled"); verify(flags).require("sensitiveMaterialUploadEnabled");
        assertThatThrownBy(() -> new ClientWriteGate(flags,cases).write(1,9,99L,false)).isInstanceOf(BusinessException.class).extracting("code").isEqualTo("MATERIAL_INPUT_INVALID");
    }
    @Test void deniedCompanyDoesNotReachCapabilityGate() {
        CapabilityService flags=mock(CapabilityService.class); CaseService cases=mock(CaseService.class);
        when(cases.clientDetail(1,9)).thenThrow(new IllegalStateException("范围不匹配"));
        assertThatThrownBy(() -> new ClientWriteGate(flags,cases).write(1,9,null,true)).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(flags);
    }
}
