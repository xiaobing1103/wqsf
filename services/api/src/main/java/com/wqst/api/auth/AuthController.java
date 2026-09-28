package com.wqst.api.auth;

import cn.dev33.satoken.stp.StpUtil;
import com.wqst.api.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService auth;
    private final WechatAuthClient wechat;
    public AuthController(AuthService auth, WechatAuthClient wechat) { this.auth = auth; this.wechat = wechat; }

    @PostMapping("/admin/login")
    ApiResponse<AuthService.LoginResult> adminLogin(@Valid @RequestBody AdminLoginRequest request) {
        return ApiResponse.ok(auth.adminLogin(request.username(), request.password()));
    }
    @PostMapping("/client/dev-login")
    ApiResponse<AuthService.LoginResult> devLogin(@Valid @RequestBody DevLoginRequest request) { return ApiResponse.ok(auth.localClientLogin(request.userId())); }
    @PostMapping("/wechat/login")
    ApiResponse<AuthService.LoginResult> wechat(@Valid @RequestBody WechatLoginRequest request) { return ApiResponse.ok(wechat.login(request.code())); }
    @GetMapping("/me")
    ApiResponse<AuthService.CurrentUser> me() { return ApiResponse.ok(auth.currentUser()); }
    @PostMapping("/logout")
    ApiResponse<Void> logout() { auth.logout(); return ApiResponse.ok(null); }

    public record AdminLoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record DevLoginRequest(@Positive Long userId) {}
    public record WechatLoginRequest(@NotBlank String code) {}
}
