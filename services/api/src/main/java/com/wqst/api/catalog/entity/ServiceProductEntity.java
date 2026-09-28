package com.wqst.api.catalog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@TableName("service_product")
public class ServiceProductEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private Long moduleId;
    private String productCode;
    private String productName;
    private String description;
    private Long templateId;
    private Integer sortOrder;
    private String status;
    private Long createdBy;
    @Version private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
