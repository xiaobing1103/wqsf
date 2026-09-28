package com.wqst.api.caseorder;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.wqst.api.common.ApiResponse;
import com.wqst.api.common.PageResponse;
import com.wqst.api.common.SecuritySupport;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/admin")
public class AdminCaseController {
    private final CaseService cases;private final ReviewService reviews;private final SecuritySupport security;
    public AdminCaseController(CaseService cases,ReviewService reviews,SecuritySupport security){this.cases=cases;this.reviews=reviews;this.security=security;}
    @GetMapping("/cases") @SaCheckPermission("case:view") ApiResponse<PageResponse<CaseService.CaseSummary>> list(@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="20")int pageSize,@RequestParam(required=false)String keyword,@RequestParam(required=false)String status,@RequestParam(required=false)String serviceType,@RequestParam(required=false)Long assigneeId){security.adminId();security.permission("case:view");return ApiResponse.ok(cases.adminList(page,pageSize,keyword,status,serviceType,assigneeId));}
    @GetMapping("/cases/{caseId}") @SaCheckPermission("case:view") ApiResponse<CaseService.CaseView> detail(@PathVariable long caseId){security.adminId();security.permission("case:view");return ApiResponse.ok(cases.adminDetail(caseId));}
    @PostMapping("/cases/{caseId}/assign") @SaCheckPermission("case:assign") ApiResponse<CaseService.CaseView> assign(@PathVariable long caseId,@Valid @RequestBody AssignRequest r){long uid=security.adminId();security.permission("case:assign");return ApiResponse.ok(cases.assign(caseId,r.assigneeId(),uid));}
    @PostMapping("/cases/{caseId}/status") @SaCheckPermission("case:review") ApiResponse<CaseService.CaseView> status(@PathVariable long caseId,@Valid @RequestBody StatusRequest r){long uid=security.adminId();security.permission("case:review");return ApiResponse.ok(cases.adminTransition(caseId,r.targetStatus(),r.note(),uid));}
    @PostMapping("/materials/{materialId}/review") @SaCheckPermission("case:review") ApiResponse<CaseService.MaterialView> review(@PathVariable long materialId,@Valid @RequestBody ReviewRequest r){long uid=security.adminId();security.permission("case:review");return ApiResponse.ok(reviews.review(materialId,new ReviewService.ReviewCommand(r.reviewStatus(),r.clientVisibleNote(),r.internalNote(),r.version()),uid));}
    @PostMapping("/cases/{caseId}/supplement-request") @SaCheckPermission("case:review") ApiResponse<ReviewService.SupplementView> supplement(@PathVariable long caseId,@Valid @RequestBody SupplementRequest r,@RequestHeader("Idempotency-Key")String key){long uid=security.adminId();security.permission("case:review");return ApiResponse.created(reviews.requestSupplement(caseId,new ReviewService.SupplementCommand(r.materialIds(),r.clientMessage()),uid,key));}
    public record AssignRequest(@Positive long assigneeId){}
    public record StatusRequest(@NotBlank String targetStatus,String note){}
    public record ReviewRequest(@NotBlank String reviewStatus,String clientVisibleNote,String internalNote,@NotNull Integer version){}
    public record SupplementRequest(@NotEmpty List<@Positive Long> materialIds,@NotBlank String clientMessage){}
}
