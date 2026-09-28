package com.wqst.api.file;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
@Getter @Setter @TableName("file_object")
public class FileObjectEntity {
    @TableId(type=IdType.AUTO) private Long id;
    private Long caseId; private String bucketName; private Long materialId; private String originalName; private String storedName;
    private String objectKey; private String contentType; private String extension; private Long fileSize; private String sha256;
    private String status; private Boolean isSensitive; private Long createdBy; private Long uploadedBy; private LocalDateTime createdAt;
}
