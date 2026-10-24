package com.wqst.api.capability;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.wqst.api.audit.AuditService;
import com.wqst.api.common.BusinessException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class CapabilityServiceTest {
    @Test void usesConfigurationWithoutDeploymentRestriction() {
        CapabilityService service=spy(new CapabilityService(mock(JdbcTemplate.class), mock(AuditService.class)));
        doReturn(List.of(new CapabilityService.Setting("tradeApplicationEnabled",true,0,false),
                new CapabilityService.Setting("categoryMatchEnabled",true,0,true))).when(service).settings();
        assertThat(service.capabilities()).hasSize(7).containsEntry("tradeApplicationEnabled",true).containsEntry("categoryMatchEnabled",true);
        service.require("tradeApplicationEnabled");
        assertThatThrownBy(() -> service.require("customerServiceEnabled")).isInstanceOf(BusinessException.class).extracting("code").isEqualTo("FEATURE_DISABLED");
    }
    @Test void disabledCategoryDisablesOcrEvenIfConfigured() {
        CapabilityService service=spy(new CapabilityService(mock(JdbcTemplate.class),mock(AuditService.class)));
        doReturn(List.of(new CapabilityService.Setting("categoryMatchEnabled",false,0,true),new CapabilityService.Setting("licenseOcrEnabled",true,0,true))).when(service).settings();
        assertThat(service.capabilities()).containsEntry("licenseOcrEnabled",false);
    }
    @Test void refusesUnknownKeysWithoutWriting() {
        JdbcTemplate jdbc=mock(JdbcTemplate.class);
        CapabilityService service=new CapabilityService(jdbc,mock(AuditService.class));
        assertThatThrownBy(() -> service.update("unknown",true,0,"测试",9)).isInstanceOf(BusinessException.class).extracting("code").isEqualTo("CAPABILITY_NOT_FOUND");
        verifyNoInteractions(jdbc);
    }
}
