package com.wqst.api.account.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@TableName("user_account")
public class UserAccountEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String accountType;
    private String username;
    private String passwordHash;
    private String wechatOpenid;
    private String wechatUnionid;
    private String displayName;
    private String phoneCipher;
    private String phoneHash;
    private String phoneMask;
    private String status;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @com.baomidou.mybatisplus.annotation.Version
    private Integer version;
    private LocalDateTime deletedAt;
}
