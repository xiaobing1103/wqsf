package com.wqst.api.company.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@TableName("company_user")
public class CompanyUserEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long companyId;
    private Long userId;
    private String memberRole;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
