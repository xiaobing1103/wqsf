package com.wqst.api.imports;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
@Getter @Setter @TableName("import_job")
public class ImportJobEntity {
    @TableId(type=IdType.AUTO) private Long id;
    private String jobNo;private Long sourceFileId;private String templateCode;private String status;private Boolean dryRun;
    private Integer totalRows;private Integer successRows;private Integer skippedRows;private Integer failedRows;private Long errorReportFileId;
    private Long operatorId;private LocalDateTime startedAt;private LocalDateTime completedAt;private LocalDateTime createdAt;
}
