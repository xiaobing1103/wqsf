package com.wqst.api.capability;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.wqst.api.common.ApiResponse;
import com.wqst.api.common.SecuritySupport;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class CapabilityController {
    private final CapabilityService service;
    private final SecuritySupport security;
    public CapabilityController(CapabilityService service, SecuritySupport security) { this.service=service; this.security=security; }

    @GetMapping("/client/capabilities")
    public ApiResponse<Map<String, Boolean>> capabilities() { return ApiResponse.ok(service.capabilities()); }

    @GetMapping("/admin/client-capabilities") @SaCheckRole("SUPER_ADMIN")
    public ApiResponse<List<CapabilityService.Setting>> settings() {
        security.adminId(); StpUtil.checkRole("SUPER_ADMIN"); return ApiResponse.ok(service.settings());
    }

    @PutMapping("/admin/client-capabilities/{key}") @SaCheckRole("SUPER_ADMIN")
    public ApiResponse<List<CapabilityService.Setting>> update(@PathVariable String key, @Valid @RequestBody UpdateRequest request) {
        long id=security.adminId(); StpUtil.checkRole("SUPER_ADMIN");
        return ApiResponse.ok(service.update(key, request.enabled(), request.version(), request.reason(), id));
    }
    public record UpdateRequest(@NotNull Boolean enabled, @NotNull @Min(0) Integer version, @NotBlank @Size(max=200) String reason) {}
}
