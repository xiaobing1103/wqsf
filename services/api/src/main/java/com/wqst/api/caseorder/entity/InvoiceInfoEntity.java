package com.wqst.api.caseorder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@TableName("invoice_info")
public class InvoiceInfoEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private Long caseId;
    private String companyName;
    private String taxNoCipher;
    private String taxNoMask;
    private String contactAddress;
    @TableField(exist = false) private String contactPhone;
    private String contactPhoneCipher;
    private String contactPhoneHash;
    private String contactPhoneMask;
    private String legalPerson;
    private String companyEmail;
    private String bankBranch;
    private String basicAccountCipher;
    private String basicAccountMask;
}
