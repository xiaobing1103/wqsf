package com.wqst.api.file;
import com.wqst.api.common.ApiResponse;
import com.wqst.api.common.SecuritySupport;
import com.wqst.api.capability.ClientWriteGate;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
@RestController @RequestMapping("/api/v1/files")
public class FileController {
    private final FileService service;private final SecuritySupport security;
    private final ClientWriteGate gate;
    public FileController(FileService service,SecuritySupport security,ClientWriteGate gate){this.service=service;this.security=security;this.gate=gate;}
    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE) ApiResponse<FileService.FileView> upload(@RequestParam long caseId,@RequestParam long materialId,@RequestPart("file")MultipartFile file){long userId=security.clientId();gate.write(caseId,userId,materialId,false);return ApiResponse.created(service.upload(caseId,materialId,file,userId));}
    @DeleteMapping("/{fileId}") ApiResponse<Void> delete(@PathVariable long fileId){service.delete(fileId,security.clientId());return ApiResponse.ok(null);}
    @PostMapping("/{fileId}/preview") ApiResponse<FileService.PreviewView> preview(@PathVariable long fileId){return ApiResponse.ok(service.preview(fileId));}
    @GetMapping ApiResponse<List<FileService.FileView>> list(@RequestParam long materialId){return ApiResponse.ok(service.list(materialId));}
}
