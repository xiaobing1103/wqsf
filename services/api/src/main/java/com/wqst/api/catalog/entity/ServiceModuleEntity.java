package com.wqst.api.catalog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@TableName("service_module")
public class ServiceModuleEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private String moduleCode;
    private String moduleName;
    private String description;
    private Integer sortOrder;
    private String status;
}
