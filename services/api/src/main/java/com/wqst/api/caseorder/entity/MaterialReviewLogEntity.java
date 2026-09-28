package com.wqst.api.caseorder.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
@Getter @Setter @TableName("material_review_log")
public class MaterialReviewLogEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private Long materialId; private Long caseId; private String beforeStatus; private String afterStatus;
    private String clientVisibleNote; private String internalNote; private Long reviewedBy; private LocalDateTime reviewedAt;
}
