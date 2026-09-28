package com.wqst.api.exports;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.wqst.api.common.ApiResponse;
import com.wqst.api.common.SecuritySupport;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/admin/exports")
public class ExportController {
    private final ExportService service;private final SecuritySupport security;
    public ExportController(ExportService service,SecuritySupport security){this.service=service;this.security=security;}
    @PostMapping("/review-excel") @SaCheckPermission("export:excel") ApiResponse<ExportService.ExportJobView> excel(@Valid @RequestBody ExportReviewRequest r,@RequestHeader("Idempotency-Key")String key){long uid=security.adminId();security.permission("export:excel");return ApiResponse.created(service.reviewExcel(r.caseIds(),uid,key));}
    @PostMapping("/material-zip") @SaCheckPermission("export:zip") ApiResponse<ExportService.ExportJobView> zip(@Valid @RequestBody ZipRequest r,@RequestHeader("Idempotency-Key")String key){long uid=security.adminId();security.permission("export:zip");if(r.includeSensitive())security.permission("file:sensitive:view");return ApiResponse.created(service.materialZip(r.caseId(),r.includeSensitive(),uid,key));}
    @GetMapping("/{jobId}") @SaCheckPermission("case:view") ApiResponse<ExportService.ExportJobView> get(@PathVariable long jobId){long uid=security.adminId();security.permission("case:view");return ApiResponse.ok(service.get(jobId,uid));}
    @PostMapping("/{jobId}/download") ApiResponse<ExportService.DownloadView> download(@PathVariable long jobId){long uid=security.adminId();ExportJobEntity job=service.requireOwned(jobId,uid);security.permission("REVIEW_EXCEL".equals(job.getExportType())?"export:excel":"export:zip");if(service.includesSensitive(job))security.permission("file:sensitive:view");return ApiResponse.ok(service.download(jobId,uid));}
    public record ExportReviewRequest(@NotEmpty List<@Positive Long> caseIds){}
    public record ZipRequest(@Positive long caseId,boolean includeSensitive){}
}
