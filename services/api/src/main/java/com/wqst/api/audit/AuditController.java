package com.wqst.api.audit;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.wqst.api.common.ApiResponse;
import com.wqst.api.common.PageResponse;
import com.wqst.api.common.SecuritySupport;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/audit")
public class AuditController {
    private final AuditService service; private final SecuritySupport security;
    public AuditController(AuditService service, SecuritySupport security) { this.service=service; this.security=security; }
    @GetMapping("/operations") @SaCheckPermission("audit:view") ApiResponse<PageResponse<com.wqst.api.audit.entity.OperationLogEntity>> operations(@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int pageSize){ security.adminId();security.permission("audit:view");return ApiResponse.ok(service.operations(page,pageSize)); }
    @GetMapping("/file-access") @SaCheckPermission("audit:view") ApiResponse<PageResponse<com.wqst.api.audit.entity.FileAccessLogEntity>> files(@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int pageSize){ security.adminId();security.permission("audit:view");return ApiResponse.ok(service.files(page,pageSize)); }
    @GetMapping("/downloads") @SaCheckPermission("audit:view") ApiResponse<PageResponse<com.wqst.api.audit.entity.DownloadLogEntity>> downloads(@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int pageSize){ security.adminId();security.permission("audit:view");return ApiResponse.ok(service.downloads(page,pageSize)); }
}
