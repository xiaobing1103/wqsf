package com.wqst.api.caseorder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@TableName("material_submission")
public class MaterialSubmissionEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private Long caseId;
    private Long templateItemId;
    private String materialCode;
    private String materialName;
    private String inputType;
    private Boolean required;
    private Boolean isSensitive;
    private String textValue;
    private String submitStatus;
    private String reviewStatus;
    private String reviewNote;
    private String clientVisibleNote;
    private String internalNote;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    @Version private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
