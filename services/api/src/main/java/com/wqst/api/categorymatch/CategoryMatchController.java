package com.wqst.api.categorymatch;

import com.wqst.api.common.ApiResponse;
import com.wqst.api.common.SecuritySupport;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/category-match")
public class CategoryMatchController {
    private final CategoryMatchService service;
    private final SecuritySupport security;

    public CategoryMatchController(CategoryMatchService service, SecuritySupport security) {
        this.service = service;
        this.security = security;
    }

    @PostMapping(value = "/recognize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ApiResponse<CategoryMatchService.BusinessLicenseView> recognize(@RequestPart("file") MultipartFile file) throws IOException {
        security.clientId();
        return ApiResponse.ok(service.recognize(file));
    }

    @PostMapping("/recommend")
    ApiResponse<CategoryMatchService.CategoryMatchView> recommend(@Valid @RequestBody CategoryMatchRequest request) {
        security.clientId();
        return ApiResponse.ok(service.recommend(request.companyName(), request.businessScope()));
    }

    public record CategoryMatchRequest(@Size(max = 128) String companyName,
                                       @NotBlank @Size(max = 5000) String businessScope) {}
}
