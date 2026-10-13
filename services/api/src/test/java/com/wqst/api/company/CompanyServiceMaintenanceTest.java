package com.wqst.api.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wqst.api.caseorder.mapper.ServiceCaseMapper;
import com.wqst.api.common.BusinessException;
import com.wqst.api.common.CryptoService;
import com.wqst.api.company.entity.CompanyEntity;
import com.wqst.api.company.entity.CompanyUserEntity;
import com.wqst.api.company.mapper.CompanyMapper;
import com.wqst.api.company.mapper.CompanyUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CompanyServiceMaintenanceTest {
    @Mock CompanyMapper companies;
    @Mock CompanyUserMapper memberships;
    @Mock ServiceCaseMapper cases;
    @Mock CryptoService crypto;
    CompanyService service;

    @BeforeEach
    void setup() {
        service = new CompanyService(companies, memberships, cases, crypto);
    }

    @Test
    void rejectsUpdateWhenVersionIsStale() {
        CompanyEntity company = company(10L, 3);
        when(companies.selectById(10L)).thenReturn(company);

        assertThatThrownBy(() -> service.update(10L, new CompanyService.UpdateCompany(
                "示例企业", "", "联系人", "", "示例法人", "湖南省", "长沙市", "", "", 2)))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("OPTIMISTIC_LOCK_CONFLICT");
    }

    @Test
    void rejectsDeleteWhenCompanyHasUnfinishedCases() {
        CompanyEntity company = company(10L, 3);
        when(companies.selectById(10L)).thenReturn(company);
        when(cases.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.softDelete(10L, 3))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("COMPANY_ACTIVE_CASE_EXISTS");
    }

    @Test
    void softDeleteDisablesCompanyAndMemberships() {
        CompanyEntity company = company(10L, 3);
        when(companies.selectById(10L)).thenReturn(company);
        when(cases.selectCount(any())).thenReturn(0L);
        when(companies.updateById(company)).thenReturn(1);

        service.softDelete(10L, 3);

        assertThat(company.getStatus()).isEqualTo("DISABLED");
        assertThat(company.getDeletedAt()).isNotNull();
        verify(memberships).disableByCompanyId(10L);
    }

    private CompanyEntity company(long id, int version) {
        CompanyEntity company = new CompanyEntity();
        company.setId(id);
        company.setVersion(version);
        company.setCompanyName("示例企业");
        company.setStatus("ENABLED");
        return company;
    }
}
