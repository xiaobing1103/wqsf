package com.wqst.api.audit.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
@Getter @Setter @TableName("operation_log")
public class OperationLogEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private Long operatorId; private String operationCode; private String businessType; private Long businessId;
    private String summary; private String requestId; private String ipAddress; private LocalDateTime createdAt;
}
