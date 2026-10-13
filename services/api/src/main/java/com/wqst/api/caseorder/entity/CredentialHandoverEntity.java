package com.wqst.api.caseorder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("credential_handover")
public class CredentialHandoverEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long caseId;
    private String onlineBankingStatus;
    private String uShieldStatus;
    private String transactionAuthorizationStatus;
    private Long handledBy;
    private LocalDateTime handledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @Version
    private Integer version;
}
