package com.wqst.api.exports;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
@Component public class ExportRecovery {
    private final ExportJobMapper jobs;private final ExportWorker worker;
    public ExportRecovery(ExportJobMapper jobs,ExportWorker worker){this.jobs=jobs;this.worker=worker;}
    @Scheduled(fixedDelayString="${wqst.export.recovery-delay-ms:15000}",initialDelayString="${wqst.export.recovery-delay-ms:15000}")
    public void recover(){jobs.resetStale();List<ExportJobEntity> pending=jobs.selectList(new LambdaQueryWrapper<ExportJobEntity>().eq(ExportJobEntity::getStatus,"PENDING").orderByAsc(ExportJobEntity::getCreatedAt).last("LIMIT 20"));pending.forEach(j->worker.process(j.getId()));}
}
