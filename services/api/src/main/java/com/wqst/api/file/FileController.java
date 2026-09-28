package com.wqst.api.file;
import com.wqst.api.common.ApiResponse;
import com.wqst.api.common.SecuritySupport;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
@RestController @RequestMapping("/api/v1/files")
public class FileController {
    private final FileService service;private final SecuritySupport security;
    public FileController(FileService service,SecuritySupport security){this.service=service;this.security=security;}
    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE) ApiResponse<FileService.FileView> upload(@RequestParam long caseId,@RequestParam long materialId,@RequestPart("file")MultipartFile file){return ApiResponse.created(service.upload(caseId,materialId,file,security.clientId()));}
    @DeleteMapping("/{fileId}") ApiResponse<Void> delete(@PathVariable long fileId){service.delete(fileId,security.clientId());return ApiResponse.ok(null);}
    @PostMapping("/{fileId}/preview") ApiResponse<FileService.PreviewView> preview(@PathVariable long fileId){return ApiResponse.ok(service.preview(fileId));}
    @GetMapping ApiResponse<List<FileService.FileView>> list(@RequestParam long materialId){return ApiResponse.ok(service.list(materialId));}
}
