package com.wqst.api.auth;

import cn.dev33.satoken.stp.StpUtil;
import com.wqst.api.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
    @PostMapping("/client/login")
    ApiResponse<AuthService.LoginResult> clientLogin(@Valid @RequestBody ClientLoginRequest request) {
        return ApiResponse.ok(auth.clientLogin(request.username(), request.password(), request.bindWechat(), request.wechatCode()));
    }
    @PostMapping("/client/dev-login")
    ApiResponse<AuthService.LoginResult> devLogin(@Valid @RequestBody DevLoginRequest request) { return ApiResponse.ok(auth.localClientLogin(request.userId())); }
    @PostMapping("/wechat/login")
    ApiResponse<AuthService.LoginResult> wechat(@Valid @RequestBody WechatLoginRequest request) {
        return ApiResponse.ok(auth.wechatLogin(wechat.exchange(request.code())));
    }
    @GetMapping("/me")
    ApiResponse<AuthService.CurrentUser> me() { return ApiResponse.ok(auth.currentUser()); }
    @PostMapping("/logout")
    ApiResponse<Void> logout() { auth.logout(); return ApiResponse.ok(null); }

    public record AdminLoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record ClientLoginRequest(@NotBlank @Size(max = 64) String username,
                                     @NotBlank @Size(max = 128) String password,
                                     boolean bindWechat,
                                     @Size(max = 256) String wechatCode) {}
    public record DevLoginRequest(@Positive Long userId) {}
    public record WechatLoginRequest(@NotBlank String code) {}
}
