package com.wqst.api.notification;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wqst.api.common.BusinessException;
import com.wqst.api.common.PageResponse;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
@Service public class NotificationService {
    private final NotificationMapper mapper;
    public NotificationService(NotificationMapper mapper){this.mapper=mapper;}
    public void send(long userId,Long companyId,Long caseId,String type,String title,String content){ NotificationEntity n=new NotificationEntity();n.setUserId(userId);n.setCompanyId(companyId);n.setCaseId(caseId);n.setNotificationType(type);n.setTitle(title);n.setContent(content);mapper.insert(n); }
    public PageResponse<NotificationEntity> mine(long userId,int page,int size){Page<NotificationEntity> r=mapper.selectPage(Page.of(page,size),new LambdaQueryWrapper<NotificationEntity>().eq(NotificationEntity::getUserId,userId).orderByDesc(NotificationEntity::getCreatedAt));return PageResponse.of(r.getRecords(),page,size,r.getTotal());}
    public void read(long id,long userId){NotificationEntity n=mapper.selectById(id);if(n==null||n.getUserId()!=userId)throw new BusinessException("NOTIFICATION_NOT_FOUND","通知不存在",HttpStatus.NOT_FOUND);n.setReadAt(LocalDateTime.now());mapper.updateById(n);}
}
