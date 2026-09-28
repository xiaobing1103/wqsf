package com.wqst.api.company.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@TableName("company")
public class CompanyEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String companyName;
    private String creditCode;
    private String contactName;
    private String legalPerson;
    private String contactPhoneCipher;
    private String contactPhoneHash;
    private String contactPhoneMask;
    private String province;
    private String city;
    private String companyEmail;
    private String detailAddress;
    private String status;
    @Version
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
