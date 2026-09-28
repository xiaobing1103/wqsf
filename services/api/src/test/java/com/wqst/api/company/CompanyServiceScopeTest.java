package com.wqst.api.company;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import com.wqst.api.common.BusinessException;
import com.wqst.api.common.CryptoService;
import com.wqst.api.company.mapper.CompanyMapper;
import com.wqst.api.company.mapper.CompanyUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
class CompanyServiceScopeTest {
    @Mock CompanyMapper companies;@Mock CompanyUserMapper memberships;@Mock CryptoService crypto;
    CompanyService service;
    @BeforeEach void setup(){MockitoAnnotations.openMocks(this);service=new CompanyService(companies,memberships,crypto);}
    @Test void enforcesCompanyMembership(){when(memberships.hasAccess(10,20)).thenReturn(0);assertThatThrownBy(()->service.requireClientAccess(10,20)).isInstanceOf(BusinessException.class).extracting("code").isEqualTo("COMPANY_SCOPE_DENIED");when(memberships.hasAccess(10,20)).thenReturn(1);assertThatCode(()->service.requireClientAccess(10,20)).doesNotThrowAnyException();}
}
