package com.wqst.api.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import cn.dev33.satoken.exception.NotRoleException;
import org.junit.jupiter.api.Test;

class GlobalExceptionHandlerTest {
    @Test void missingRoleIsForbiddenInsteadOfInternalError() {
        var response=new GlobalExceptionHandler().noRole(mock(NotRoleException.class));
        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getBody().code()).isEqualTo("AUTH_PERMISSION_DENIED");
    }
}
