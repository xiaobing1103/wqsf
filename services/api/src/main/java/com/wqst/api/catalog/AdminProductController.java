package com.wqst.api.catalog;
import cn.dev33.satoken.annotation.SaCheckPermission;

import com.wqst.api.common.ApiResponse;
import com.wqst.api.common.SecuritySupport;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminProductController {
    private final CatalogService service;
    private final SecuritySupport security;
    public AdminProductController(CatalogService service, SecuritySupport security) { this.service = service; this.security = security; }

    @GetMapping("/products") @SaCheckPermission("product:view")
    ApiResponse<List<CatalogService.ProductAdminView>> list() {
        security.adminId(); security.permission("product:view"); return ApiResponse.ok(service.adminProducts());
    }
    @PostMapping("/products") @SaCheckPermission("product:edit")
    ApiResponse<CatalogService.ProductAdminView> create(@Valid @RequestBody ProductRequest r) {
        long userId = security.adminId(); security.permission("product:edit"); return ApiResponse.created(service.create(command(r), userId));
    }
    @PatchMapping("/products/{productId}") @SaCheckPermission("product:edit")
    ApiResponse<CatalogService.ProductAdminView> update(@PathVariable long productId, @Valid @RequestBody ProductRequest r) {
        security.adminId(); security.permission("product:edit"); return ApiResponse.ok(service.update(productId, command(r), r.version()));
    }
    @PostMapping("/products/{productId}/publish") @SaCheckPermission("product:edit")
    ApiResponse<CatalogService.ProductAdminView> publish(@PathVariable long productId) {
        security.adminId(); security.permission("product:edit"); return ApiResponse.ok(service.status(productId, "PUBLISHED"));
    }
    @PostMapping("/products/{productId}/unpublish") @SaCheckPermission("product:edit")
    ApiResponse<CatalogService.ProductAdminView> unpublish(@PathVariable long productId) {
        security.adminId(); security.permission("product:edit"); return ApiResponse.ok(service.status(productId, "UNPUBLISHED"));
    }
    @PostMapping("/templates/{sourceTemplateId}/versions") @SaCheckPermission("product:edit")
    ApiResponse<CatalogService.TemplateView> createVersion(@PathVariable long sourceTemplateId, @Valid @RequestBody TemplateVersionRequest r) {
        long userId = security.adminId(); security.permission("product:edit");
        List<CatalogService.TemplateItemCommand> items = r.items().stream().map(i -> new CatalogService.TemplateItemCommand(i.itemCode(),
                i.itemName(), i.inputType(), i.required(), i.sensitive(), i.allowedExtensions(), i.maxFileSizeMb(), i.maxFileCount(), i.helpText())).toList();
        return ApiResponse.created(service.createTemplateVersion(sourceTemplateId, r.templateName(), items, userId));
    }
    @PostMapping("/templates/{templateId}/publish") @SaCheckPermission("product:edit")
    ApiResponse<CatalogService.TemplateView> publishTemplate(@PathVariable long templateId) {
        security.adminId(); security.permission("product:edit"); return ApiResponse.ok(service.publishTemplate(templateId));
    }

    private CatalogService.ProductCommand command(ProductRequest r) { return new CatalogService.ProductCommand(r.moduleId(), r.productCode(),
            r.productName(), r.description(), r.templateId(), r.sortOrder()); }
    public record ProductRequest(@NotNull @Positive Long moduleId, @NotBlank String productCode, @NotBlank String productName,
            String description, @NotNull @Positive Long templateId, @NotNull Integer sortOrder, int version) {}
    public record TemplateVersionRequest(@NotBlank String templateName, @NotNull List<@Valid TemplateItemRequest> items) {}
    public record TemplateItemRequest(@NotBlank String itemCode, @NotBlank String itemName, @NotBlank String inputType,
            boolean required, boolean sensitive, String allowedExtensions, Integer maxFileSizeMb,
            @NotNull @Positive Integer maxFileCount, String helpText) {}
}
