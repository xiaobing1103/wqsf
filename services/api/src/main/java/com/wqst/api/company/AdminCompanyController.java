package com.wqst.api.company;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;

import com.wqst.api.account.AccountService;
import com.wqst.api.audit.AuditService;
import com.wqst.api.common.ApiResponse;
import com.wqst.api.common.PageResponse;
import com.wqst.api.common.SecuritySupport;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/companies")
public class AdminCompanyController {
    private static final Set<String> MEMBER_ROLES = Set.of("OWNER", "CONTACT", "VIEWER");
    private final CompanyService service;
    private final AccountService accounts;
    private final SecuritySupport security;
    private final AuditService audit;
    public AdminCompanyController(CompanyService service, AccountService accounts, SecuritySupport security, AuditService audit) {
        this.service = service; this.accounts = accounts; this.security = security; this.audit = audit;
    }

    @GetMapping @SaCheckPermission("company:view")
    ApiResponse<PageResponse<CompanyService.CompanyView>> list(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize, @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        security.adminId(); security.permission("company:view");
        return ApiResponse.ok(service.adminList(page, pageSize, keyword, status));
    }

    @GetMapping("/{companyId}") @SaCheckPermission("company:view")
    ApiResponse<CompanyService.CompanyView> detail(@PathVariable long companyId) {
        security.adminId(); security.permission("company:view");
        return ApiResponse.ok(service.adminDetail(companyId));
    }

    @PostMapping @SaCheckPermission("company:view")
    ApiResponse<CompanyService.CompanyView> create(@Valid @RequestBody CompanyRequest request) {
        long operatorId = security.adminId(); security.permission("company:view");
        CompanyService.CompanyView company = service.create(new CompanyService.CreateCompany(request.companyName(), request.creditCode(),
                request.contactName(), request.contactPhone(), request.legalPerson(), request.province(), request.city(),
                request.companyEmail(), request.detailAddress()));
        audit.operation(operatorId, "COMPANY_CREATE", "COMPANY", company.id(), "创建企业客户");
        return ApiResponse.created(company);
    }

    @PatchMapping("/{companyId}") @SaCheckPermission("company:view")
    ApiResponse<CompanyService.CompanyView> update(@PathVariable long companyId,
            @Valid @RequestBody CompanyUpdateRequest request) {
        long operatorId = security.adminId(); security.permission("company:view");
        CompanyService.CompanyView company = service.update(companyId, new CompanyService.UpdateCompany(
                request.companyName(), request.creditCode(), request.contactName(), request.contactPhone(),
                request.legalPerson(), request.province(), request.city(), request.companyEmail(),
                request.detailAddress(), request.version()));
        audit.operation(operatorId, "COMPANY_UPDATE", "COMPANY", companyId, "更新企业客户资料");
        return ApiResponse.ok(company);
    }

    @DeleteMapping("/{companyId}") @SaCheckRole("SUPER_ADMIN")
    ApiResponse<Void> delete(@PathVariable long companyId, @RequestParam int version) {
        long operatorId = security.adminId();
        service.softDelete(companyId, version);
        audit.operation(operatorId, "COMPANY_DELETE", "COMPANY", companyId, "软删除企业客户");
        return ApiResponse.ok(null);
    }

    @PostMapping("/{companyId}/members") @SaCheckPermission("company:view")
    ApiResponse<Void> addMember(@PathVariable long companyId, @Valid @RequestBody MemberRequest request) {
        security.adminId(); security.permission("company:view");
        if (!MEMBER_ROLES.contains(request.memberRole())) throw new IllegalArgumentException("memberRole 无效");
        service.addMember(companyId, request.userId(), request.memberRole());
        return ApiResponse.ok(null);
    }

    @GetMapping("/{companyId}/client-accounts") @SaCheckPermission("company:view")
    ApiResponse<List<AccountService.AccountView>> clientAccounts(@PathVariable long companyId) {
        security.adminId(); security.permission("company:view");
        return ApiResponse.ok(accounts.clientsByIds(service.enabledMemberUserIds(companyId)));
    }

    @PostMapping("/{companyId}/client-accounts") @SaCheckPermission("company:view") @Transactional
    ApiResponse<AccountService.AccountView> createClientAccount(@PathVariable long companyId,
            @Valid @RequestBody ClientAccountRequest request) {
        long operatorId = security.adminId(); security.permission("company:view");
        if (!MEMBER_ROLES.contains(request.memberRole())) throw new IllegalArgumentException("memberRole 无效");
        service.require(companyId);
        AccountService.AccountView account = accounts.create(new AccountService.CreateAccount("CLIENT", request.username(),
                request.password(), request.displayName(), request.phone(), List.of("CLIENT")));
        service.addMember(companyId, account.id(), request.memberRole());
        audit.operation(operatorId, "CLIENT_ACCOUNT_CREATE", "USER_ACCOUNT", account.id(), "为企业创建客户登录账号");
        return ApiResponse.created(account);
    }

    @PutMapping("/{companyId}/client-accounts/{userId}/password") @SaCheckPermission("company:view")
    ApiResponse<AccountService.AccountView> resetClientPassword(@PathVariable long companyId, @PathVariable long userId,
            @Valid @RequestBody ClientPasswordRequest request) {
        long operatorId = security.adminId(); security.permission("company:view");
        service.requireClientAccess(companyId, userId);
        AccountService.AccountView account = accounts.resetClientPassword(userId, request.password());
        audit.operation(operatorId, "CLIENT_PASSWORD_RESET", "USER_ACCOUNT", userId, "重置企业客户登录密码");
        return ApiResponse.ok(account);
    }

    public record CompanyRequest(@NotBlank String companyName, String creditCode, String contactName,
            @Pattern(regexp = "^$|^[0-9+ -]{7,20}$", message = "联系电话格式不正确") String contactPhone,
            String legalPerson, String province, String city, @Email String companyEmail, String detailAddress) {}
    public record CompanyUpdateRequest(@NotBlank String companyName, String creditCode, String contactName,
            @Pattern(regexp = "^$|^[0-9+ -]{7,20}$", message = "联系电话格式不正确") String contactPhone,
            String legalPerson, String province, String city, @Email String companyEmail, String detailAddress,
            @NotNull Integer version) {}
    public record MemberRequest(long userId, @NotBlank String memberRole) {}
    public record ClientAccountRequest(@NotBlank @Size(max = 64) String username,
            @NotBlank @Size(min = 8, max = 128) String password,
            @NotBlank @Size(max = 64) String displayName,
            @Pattern(regexp = "^$|^[0-9+ -]{7,20}$", message = "联系电话格式不正确") String phone,
            @NotBlank String memberRole) {}
    public record ClientPasswordRequest(@NotBlank @Size(min = 8, max = 128) String password) {}
}
