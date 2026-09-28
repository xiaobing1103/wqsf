package com.wqst.api.company;

import com.wqst.api.common.ApiResponse;
import com.wqst.api.common.SecuritySupport;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/companies")
public class CompanyController {
    private final CompanyService service;
    private final SecuritySupport security;
    public CompanyController(CompanyService service, SecuritySupport security) { this.service = service; this.security = security; }

    @GetMapping("/my")
    ApiResponse<List<CompanyService.CompanyView>> mine() { return ApiResponse.ok(service.mine(security.clientId())); }

    @GetMapping("/{companyId}")
    ApiResponse<CompanyService.CompanyView> detail(@PathVariable long companyId) {
        return ApiResponse.ok(service.clientDetail(companyId, security.clientId()));
    }
}
