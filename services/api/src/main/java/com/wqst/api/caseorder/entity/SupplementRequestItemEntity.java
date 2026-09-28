package com.wqst.api.caseorder.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
@Getter @Setter @TableName("supplement_request_item")
public class SupplementRequestItemEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private Long requestId; private Long materialId;
}
