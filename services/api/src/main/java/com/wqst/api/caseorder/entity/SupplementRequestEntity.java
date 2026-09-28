package com.wqst.api.caseorder.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
@Getter @Setter @TableName("supplement_request")
public class SupplementRequestEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private Long caseId; private String requestNo; private String clientMessage; private String status;
    private Long requestedBy; private LocalDateTime requestedAt; private LocalDateTime resolvedAt;
}
