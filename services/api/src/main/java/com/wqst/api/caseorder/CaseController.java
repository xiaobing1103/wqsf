package com.wqst.api.caseorder;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.wqst.api.common.ApiResponse;
import com.wqst.api.common.PageResponse;
import com.wqst.api.common.SecuritySupport;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cases")
public class CaseController {
    private final CaseService service; private final ReviewService reviews; private final SecuritySupport security;
    public CaseController(CaseService service,ReviewService reviews,SecuritySupport security){this.service=service;this.reviews=reviews;this.security=security;}

    @PostMapping @SaCheckPermission(value = "case:create", type = "client")
    ApiResponse<CaseService.CaseView> create(@Valid @RequestBody CreateRequest r,@RequestHeader("Idempotency-Key") String key){long userId=security.clientId();security.clientPermission("case:create");return ApiResponse.created(service.create(new CaseService.CreateCase(r.companyId(),r.productId(),r.serviceType(),r.caseMonth()),userId,key));}
    @GetMapping("/my")
    ApiResponse<PageResponse<CaseService.CaseSummary>> mine(@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="20")int pageSize,@RequestParam(required=false)String status){return ApiResponse.ok(service.mine(security.clientId(),page,pageSize,status));}
    @GetMapping("/{caseId}") ApiResponse<CaseService.CaseView> detail(@PathVariable long caseId){return ApiResponse.ok(service.clientDetail(caseId,security.clientId()));}
    @PutMapping("/{caseId}/invoice-info") ApiResponse<CaseService.InvoiceView> invoice(@PathVariable long caseId,@Valid @RequestBody InvoiceRequest r){return ApiResponse.ok(service.saveInvoice(caseId,new CaseService.InvoiceCommand(r.companyName(),r.taxNo(),r.contactAddress(),r.contactPhone(),r.legalPerson(),r.companyEmail(),r.bankBranch(),r.basicAccount()),security.clientId()));}
    @PutMapping("/{caseId}/simple-info") ApiResponse<CaseService.CaseView> simple(@PathVariable long caseId,@Valid @RequestBody SimpleRequest r){return ApiResponse.ok(service.saveSimple(caseId,new CaseService.SimpleCommand(r.legalPersonName(),r.companyName(),r.contactPhone(),r.consent()),security.clientId()));}
    @PutMapping("/{caseId}/materials/{materialId}/text") ApiResponse<CaseService.MaterialView> text(@PathVariable long caseId,@PathVariable long materialId,@Valid @RequestBody TextRequest r){return ApiResponse.ok(service.saveText(caseId,materialId,r.value(),security.clientId()));}
    @PostMapping("/{caseId}/submit") ApiResponse<CaseService.CaseView> submit(@PathVariable long caseId,@RequestHeader("Idempotency-Key")String key){long userId=security.clientId();return ApiResponse.ok(service.submit(caseId,userId,key));}
    @GetMapping("/{caseId}/timeline") ApiResponse<List<CaseService.TimelineView>> timeline(@PathVariable long caseId){return ApiResponse.ok(service.timeline(caseId,security.clientId()));}

    public record CreateRequest(@Positive long companyId,@Positive long productId,@Pattern(regexp="TRADE_INCREMENT|IP|QUALIFICATION")String serviceType,@Pattern(regexp="^$|^\\d{4}-(0[1-9]|1[0-2])$")String caseMonth){}
    public record InvoiceRequest(@NotBlank String companyName,@NotBlank String taxNo,@NotBlank String contactAddress,@NotBlank @Pattern(regexp="^[0-9+ -]{7,20}$")String contactPhone,@NotBlank String legalPerson,@NotBlank @Email String companyEmail,@NotBlank String bankBranch,@NotBlank String basicAccount){}
    public record SimpleRequest(@NotBlank String legalPersonName,@NotBlank String companyName,@NotBlank @Pattern(regexp="^[0-9+ -]{7,20}$")String contactPhone,boolean consent){}
    public record TextRequest(@NotBlank String value){}
}
