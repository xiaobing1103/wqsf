package com.wqst.api.company;
import cn.dev33.satoken.annotation.SaCheckPermission;

import com.wqst.api.common.ApiResponse;
import com.wqst.api.common.PageResponse;
import com.wqst.api.common.SecuritySupport;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.Set;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/companies")
public class AdminCompanyController {
    private static final Set<String> MEMBER_ROLES = Set.of("OWNER", "CONTACT", "VIEWER");
    private final CompanyService service;
    private final SecuritySupport security;
    public AdminCompanyController(CompanyService service, SecuritySupport security) { this.service = service; this.security = security; }

    @GetMapping @SaCheckPermission("company:view")
    ApiResponse<PageResponse<CompanyService.CompanyView>> list(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize, @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        security.adminId(); security.permission("company:view");
        return ApiResponse.ok(service.adminList(page, pageSize, keyword, status));
    }

    @PostMapping @SaCheckPermission("company:view")
    ApiResponse<CompanyService.CompanyView> create(@Valid @RequestBody CompanyRequest request) {
        security.adminId(); security.permission("company:view");
        return ApiResponse.created(service.create(new CompanyService.CreateCompany(request.companyName(), request.creditCode(),
                request.contactName(), request.contactPhone(), request.legalPerson(), request.province(), request.city(),
                request.companyEmail(), request.detailAddress())));
    }

    @PostMapping("/{companyId}/members") @SaCheckPermission("company:view")
    ApiResponse<Void> addMember(@PathVariable long companyId, @Valid @RequestBody MemberRequest request) {
        security.adminId(); security.permission("company:view");
        if (!MEMBER_ROLES.contains(request.memberRole())) throw new IllegalArgumentException("memberRole 无效");
        service.addMember(companyId, request.userId(), request.memberRole());
        return ApiResponse.ok(null);
    }

    public record CompanyRequest(@NotBlank String companyName, String creditCode, String contactName,
            @Pattern(regexp = "^$|^[0-9+ -]{7,20}$", message = "联系电话格式不正确") String contactPhone,
            String legalPerson, String province, String city, @Email String companyEmail, String detailAddress) {}
    public record MemberRequest(long userId, @NotBlank String memberRole) {}
}
