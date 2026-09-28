package com.wqst.api.dashboard;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.wqst.api.common.ApiResponse;
import com.wqst.api.common.SecuritySupport;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController @RequestMapping("/api/v1/admin/dashboard")
public class DashboardController {
    private final DashboardService service;private final SecuritySupport security;
    public DashboardController(DashboardService service,SecuritySupport security){this.service=service;this.security=security;}
    @GetMapping @SaCheckPermission("dashboard:view") ApiResponse<DashboardService.DashboardView> dashboard(){security.adminId();security.permission("dashboard:view");return ApiResponse.ok(service.view());}
}
