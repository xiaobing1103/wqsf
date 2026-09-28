package com.wqst.api.imports;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
@Getter @Setter @TableName("import_job_error")
public class ImportJobErrorEntity {
    @TableId(type=IdType.AUTO) private Long id;
    private Long jobId;
    @TableField("row_no") private Integer rowNumber;
    private String errorCode;private String errorMessage;private String rowSnapshot;private LocalDateTime createdAt;
}
