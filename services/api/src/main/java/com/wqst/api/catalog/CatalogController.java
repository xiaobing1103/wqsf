package com.wqst.api.catalog;

import com.wqst.api.common.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class CatalogController {
    private final CatalogService service;
    public CatalogController(CatalogService service) { this.service = service; }

    @GetMapping("/services/catalog")
    ApiResponse<List<CatalogService.ModuleView>> catalog() { return ApiResponse.ok(service.catalog()); }

    @GetMapping("/products/{productId}/material-template")
    ApiResponse<CatalogService.TemplateView> template(@PathVariable long productId) { return ApiResponse.ok(service.templateForProduct(productId)); }
}
