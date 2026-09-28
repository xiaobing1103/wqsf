package com.wqst.api.notification;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
@Getter @Setter @TableName("notification")
public class NotificationEntity {
    @TableId(type=IdType.AUTO) private Long id;
    private Long userId; private Long companyId; private Long caseId; private String notificationType; private String title;
    private String content; private LocalDateTime readAt; private LocalDateTime createdAt;
}
