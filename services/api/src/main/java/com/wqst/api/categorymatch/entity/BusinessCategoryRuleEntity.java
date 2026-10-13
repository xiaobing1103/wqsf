package com.wqst.api.categorymatch.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("business_category_rule")
public class BusinessCategoryRuleEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String industryCode;
    private String industryName;
    private String categoryCode;
    private String categoryName;
    private String keywords;
    private String serviceProductCode;
    private Integer sortOrder;
    private String status;
}
