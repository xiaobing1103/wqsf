package com.wqst.api.catalog;

import com.wqst.api.common.ApiResponse;
import com.wqst.api.capability.CapabilityService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class CatalogController {
    private final CatalogService service;
    private final CapabilityService capabilities;
    public CatalogController(CatalogService service, CapabilityService capabilities) { this.service = service; this.capabilities=capabilities; }

    @GetMapping("/services/catalog")
    ApiResponse<List<CatalogService.ModuleView>> catalog() {
        var flags=capabilities.capabilities();
        return ApiResponse.ok(service.catalog().stream().filter(m -> Boolean.TRUE.equals(flags.get(capabilities.serviceKey(m.moduleCode())))).toList());
    }

    @GetMapping("/products/{productId}/material-template")
    ApiResponse<CatalogService.TemplateView> template(@PathVariable long productId) { return ApiResponse.ok(service.templateForProduct(productId)); }
}
