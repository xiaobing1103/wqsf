package com.wqst.api.imports;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
@Component public class ImportRecovery {
    private final ImportJobMapper jobs;private final ImportWorker worker;
    public ImportRecovery(ImportJobMapper jobs,ImportWorker worker){this.jobs=jobs;this.worker=worker;}
    @Scheduled(fixedDelayString="${wqst.import.recovery-delay-ms:15000}",initialDelayString="${wqst.import.recovery-delay-ms:15000}")
    public void recover(){jobs.resetStale();List<ImportJobEntity> pending=jobs.selectList(new LambdaQueryWrapper<ImportJobEntity>().eq(ImportJobEntity::getStatus,"PENDING").orderByAsc(ImportJobEntity::getCreatedAt).last("LIMIT 20"));pending.forEach(j->worker.process(j.getId()));}
}
