package com.wqst.api.catalog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@TableName("material_template")
public class MaterialTemplateEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private String templateCode;
    private String templateName;
    private Integer versionNo;
    private String status;
    private Long createdBy;
    private LocalDateTime publishedAt;
}
