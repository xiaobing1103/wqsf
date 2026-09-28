package com.wqst.api.audit.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
@Getter @Setter @TableName("download_log")
public class DownloadLogEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private Long exportJobId; private Long operatorId; private String result; private String ipAddress;
    private String userAgent; private LocalDateTime createdAt;
}
