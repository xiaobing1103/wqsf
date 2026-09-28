package com.wqst.api.imports;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.wqst.api.common.ApiResponse;
import com.wqst.api.common.SecuritySupport;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
@RestController @RequestMapping("/api/v1/admin/imports")
public class ImportController {
    private final ImportService service;private final SecuritySupport security;
    public ImportController(ImportService service,SecuritySupport security){this.service=service;this.security=security;}
    @GetMapping("/template") @SaCheckPermission("company:import") ApiResponse<ImportService.TemplateDownload> template(){long uid=security.adminId();security.permission("company:import");return ApiResponse.ok(service.template(uid));}
    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @SaCheckPermission("company:import") ApiResponse<ImportService.JobView> create(@RequestPart("file")MultipartFile file,@RequestParam(defaultValue="false")boolean dryRun,@RequestHeader("Idempotency-Key")String key){long uid=security.adminId();security.permission("company:import");return ApiResponse.created(service.create(file,dryRun,uid,key));}
    @GetMapping("/{jobId}") @SaCheckPermission("company:import") ApiResponse<ImportService.JobView> get(@PathVariable long jobId){long uid=security.adminId();security.permission("company:import");return ApiResponse.ok(service.get(jobId,uid));}
    @PostMapping("/{jobId}/error-report") @SaCheckPermission("company:import") ApiResponse<ImportService.TemplateDownload> error(@PathVariable long jobId){long uid=security.adminId();security.permission("company:import");return ApiResponse.ok(service.errorReport(jobId,uid));}
}
