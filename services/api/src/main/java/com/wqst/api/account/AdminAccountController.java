package com.wqst.api.account;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.wqst.api.common.ApiResponse;
import com.wqst.api.common.SecuritySupport;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/admin/accounts")
public class AdminAccountController {
    private final AccountService service;private final SecuritySupport security;
    public AdminAccountController(AccountService service,SecuritySupport security){this.service=service;this.security=security;}
    @GetMapping @SaCheckRole("SUPER_ADMIN") ApiResponse<List<AccountService.AccountView>> list(){security.adminId();StpUtil.checkRole("SUPER_ADMIN");return ApiResponse.ok(service.list());}
    @PostMapping @SaCheckRole("SUPER_ADMIN") ApiResponse<AccountService.AccountView> create(@Valid @RequestBody CreateRequest r){security.adminId();StpUtil.checkRole("SUPER_ADMIN");return ApiResponse.created(service.create(new AccountService.CreateAccount(r.accountType(),r.username(),r.password(),r.displayName(),r.phone(),r.roles())));}
    @PutMapping("/{userId}/roles") @SaCheckRole("SUPER_ADMIN") ApiResponse<AccountService.AccountView> roles(@PathVariable long userId,@Valid @RequestBody RolesRequest r){security.adminId();StpUtil.checkRole("SUPER_ADMIN");return ApiResponse.ok(service.roles(userId,r.roles()));}
    public record CreateRequest(@NotBlank String accountType,String username,String password,@NotBlank String displayName,String phone,@NotEmpty List<@NotBlank String> roles){}
    public record RolesRequest(@NotEmpty List<@NotBlank String> roles){}
}
