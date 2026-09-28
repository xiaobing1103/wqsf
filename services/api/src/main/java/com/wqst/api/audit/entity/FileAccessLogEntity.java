package com.wqst.api.audit.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
@Getter @Setter @TableName("file_access_log")
public class FileAccessLogEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private Long fileId; private Long caseId; private String accessType; private Long operatorId; private String result;
    private String requestId; private String ipAddress; private LocalDateTime createdAt;
}
