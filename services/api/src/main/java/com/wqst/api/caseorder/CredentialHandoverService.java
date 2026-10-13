package com.wqst.api.caseorder;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wqst.api.audit.AuditService;
import com.wqst.api.caseorder.entity.CredentialHandoverEntity;
import com.wqst.api.caseorder.entity.ServiceCaseEntity;
import com.wqst.api.caseorder.mapper.CredentialHandoverMapper;
import com.wqst.api.common.BusinessException;
import java.time.LocalDateTime;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CredentialHandoverService {
    private static final Set<String> STATUSES = Set.of("PENDING", "HANDED_OVER", "VERIFIED", "RETURNED", "NOT_REQUIRED");
    private final CredentialHandoverMapper handovers;
    private final CaseService cases;
    private final AuditService audit;

    public CredentialHandoverService(CredentialHandoverMapper handovers, CaseService cases, AuditService audit) {
        this.handovers = handovers;
        this.cases = cases;
        this.audit = audit;
    }

    public CredentialHandoverView get(long caseId) {
        requireTradeCase(caseId);
        CredentialHandoverEntity entity = find(caseId);
        return entity == null
                ? new CredentialHandoverView(null, caseId, "PENDING", "PENDING", "PENDING", null, null, null, 0)
                : view(entity);
    }

    @Transactional
    public CredentialHandoverView save(long caseId, CredentialHandoverCommand command, long operatorId) {
        requireTradeCase(caseId);
        validate(command.onlineBankingStatus());
        validate(command.uShieldStatus());
        validate(command.transactionAuthorizationStatus());
        CredentialHandoverEntity entity = find(caseId);
        LocalDateTime now = LocalDateTime.now();
        if (entity == null) {
            if (command.version() != 0) throw optimistic();
            entity = new CredentialHandoverEntity();
            entity.setCaseId(caseId);
            entity.setVersion(0);
            apply(entity, command, operatorId, now);
            handovers.insert(entity);
        } else {
            if (!entity.getVersion().equals(command.version())) throw optimistic();
            apply(entity, command, operatorId, now);
            if (handovers.updateById(entity) == 0) throw optimistic();
        }
        audit.operation(operatorId, "CREDENTIAL_HANDOVER_UPDATE", "CASE", caseId, "更新线下凭证交接状态（未保存密码）");
        return view(find(caseId));
    }

    private void apply(CredentialHandoverEntity entity, CredentialHandoverCommand command, long operatorId, LocalDateTime now) {
        entity.setOnlineBankingStatus(command.onlineBankingStatus());
        entity.setUShieldStatus(command.uShieldStatus());
        entity.setTransactionAuthorizationStatus(command.transactionAuthorizationStatus());
        entity.setHandledBy(operatorId);
        entity.setHandledAt(now);
    }

    private CredentialHandoverEntity find(long caseId) {
        return handovers.selectOne(new LambdaQueryWrapper<CredentialHandoverEntity>()
                .eq(CredentialHandoverEntity::getCaseId, caseId));
    }

    private void requireTradeCase(long caseId) {
        ServiceCaseEntity serviceCase = cases.require(caseId);
        if (!"TRADE_INCREMENT".equals(serviceCase.getServiceType())) {
            throw new BusinessException("CREDENTIAL_HANDOVER_NOT_APPLICABLE", "该报单不需要线下凭证交接", HttpStatus.CONFLICT);
        }
    }

    private void validate(String status) {
        if (!STATUSES.contains(status)) {
            throw new BusinessException("CREDENTIAL_HANDOVER_STATUS_INVALID", "线下凭证交接状态无效", HttpStatus.BAD_REQUEST);
        }
    }

    private BusinessException optimistic() {
        return new BusinessException("OPTIMISTIC_LOCK_CONFLICT", "数据已被其他管理员修改，请刷新后重试", HttpStatus.CONFLICT);
    }

    private CredentialHandoverView view(CredentialHandoverEntity entity) {
        return new CredentialHandoverView(entity.getId(), entity.getCaseId(), entity.getOnlineBankingStatus(),
                entity.getUShieldStatus(), entity.getTransactionAuthorizationStatus(), entity.getHandledBy(),
                entity.getHandledAt(), entity.getUpdatedAt(), entity.getVersion());
    }

    public record CredentialHandoverCommand(String onlineBankingStatus, String uShieldStatus,
                                              String transactionAuthorizationStatus, int version) {}

    public record CredentialHandoverView(Long id, Long caseId, String onlineBankingStatus, String uShieldStatus,
                                           String transactionAuthorizationStatus, Long handledBy,
                                           LocalDateTime handledAt, LocalDateTime updatedAt, Integer version) {}
}
