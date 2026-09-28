package com.wqst.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.wqst.api.account.AdminAccountController;
import com.wqst.api.audit.AuditController;
import com.wqst.api.auth.AuthController;
import com.wqst.api.caseorder.AdminCaseController;
import com.wqst.api.caseorder.CaseController;
import com.wqst.api.catalog.AdminProductController;
import com.wqst.api.catalog.CatalogController;
import com.wqst.api.company.AdminCompanyController;
import com.wqst.api.company.CompanyController;
import com.wqst.api.dashboard.DashboardController;
import com.wqst.api.exports.ExportController;
import com.wqst.api.file.FileController;
import com.wqst.api.imports.ImportController;
import com.wqst.api.notification.NotificationController;
import io.swagger.v3.oas.models.Operation;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

class OpenApiConfigTest {
    private static final List<Class<?>> CONTROLLERS = List.of(
            AdminAccountController.class, AuditController.class, AuthController.class,
            AdminCaseController.class, CaseController.class, AdminProductController.class,
            CatalogController.class, AdminCompanyController.class, CompanyController.class,
            DashboardController.class, ExportController.class, FileController.class,
            ImportController.class, NotificationController.class);

    @Test
    void openApiMetadataUsesChineseContractAndBearerAuth() {
        OpenAPIAssertions assertions = new OpenAPIAssertions(new OpenApiConfig().wanqiOpenApi());
        assertThat(assertions.openapi().getInfo().getTitle()).isEqualTo("万企商服通接口文档");
        assertThat(assertions.openapi().getInfo().getVersion()).isEqualTo("1.0.0");
        assertThat(assertions.openapi().getComponents().getSecuritySchemes()).containsKey("BearerAuth");
        assertThat(assertions.openapi().getSecurity()).isNotEmpty();
    }

    @Test
    void everyControllerOperationGetsStableIdAndChineseSummary() {
        OpenApiConfig config = new OpenApiConfig();
        Set<String> ids = new HashSet<>();
        int count = 0;
        for (Class<?> controller : CONTROLLERS) {
            for (Method method : controller.getDeclaredMethods()) {
                if (!isEndpoint(method)) continue;
                Operation operation = config.customize(new Operation(), controller, method);
                assertThat(operation.getOperationId()).doesNotContain("_1");
                assertThat(operation.getSummary()).containsPattern("[\\u4e00-\\u9fff]");
                assertThat(operation.getTags()).allMatch(tag -> tag.matches(".*[\\u4e00-\\u9fff].*"));
                assertThat(ids.add(operation.getOperationId()))
                        .as("operationId 不得重复：" + operation.getOperationId()).isTrue();
                count++;
            }
        }
        assertThat(count).isEqualTo(54);
    }

    private boolean isEndpoint(Method method) {
        return method.isAnnotationPresent(GetMapping.class) || method.isAnnotationPresent(PostMapping.class)
                || method.isAnnotationPresent(PutMapping.class) || method.isAnnotationPresent(PatchMapping.class)
                || method.isAnnotationPresent(DeleteMapping.class);
    }

    private record OpenAPIAssertions(io.swagger.v3.oas.models.OpenAPI openapi) {}
}
