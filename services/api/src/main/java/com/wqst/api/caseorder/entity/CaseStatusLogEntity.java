package com.wqst.api.caseorder.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
@Getter @Setter @TableName("case_status_log")
public class CaseStatusLogEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private Long caseId; private String beforeStatus; private String afterStatus; private String note; private Long changedBy;
    private LocalDateTime createdAt;
}
