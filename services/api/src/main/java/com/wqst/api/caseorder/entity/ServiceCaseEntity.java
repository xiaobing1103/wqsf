package com.wqst.api.caseorder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@TableName("service_case")
public class ServiceCaseEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private String caseNo;
    private Long companyId;
    private Long productId;
    private String serviceType;
    private String status;
    private Long assigneeId;
    private Long applicantUserId;
    private String companyNameSnapshot;
    private String legalPersonName;
    private String contactPhoneCipher;
    private String contactPhoneHash;
    private String contactPhoneMask;
    private String caseMonth;
    private LocalDateTime submittedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @Version private Integer version;
    private LocalDateTime deletedAt;
}
