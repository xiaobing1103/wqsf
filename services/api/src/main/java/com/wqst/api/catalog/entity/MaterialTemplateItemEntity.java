package com.wqst.api.catalog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@TableName("material_template_item")
public class MaterialTemplateItemEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private Long templateId;
    private String itemCode;
    private String itemName;
    private String inputType;
    private Boolean isRequired;
    private Boolean isSensitive;
    private String allowedExtensions;
    private Integer maxFileSizeMb;
    private Integer maxFileCount;
    private String helpText;
    private Integer sortOrder;
}
