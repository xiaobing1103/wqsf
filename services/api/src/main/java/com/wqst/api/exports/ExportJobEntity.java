package com.wqst.api.exports;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
@Getter @Setter @TableName("export_job")
public class ExportJobEntity {
    @TableId(type=IdType.AUTO) private Long id;
    private Long caseId;private String requestId;private String requestPayload;private String exportType;private String status;
    private String objectKey;private String errorCode;private String errorMessage;private LocalDateTime expiresAt;private LocalDateTime startedAt;
    private LocalDateTime completedAt;private Integer downloadCount;private Long operatorId;private LocalDateTime createdAt;@Version private Integer version;
}
