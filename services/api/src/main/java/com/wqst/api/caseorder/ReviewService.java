package com.wqst.api.caseorder;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.wqst.api.audit.AuditService;
import com.wqst.api.caseorder.entity.MaterialReviewLogEntity;
import com.wqst.api.caseorder.entity.MaterialSubmissionEntity;
import com.wqst.api.caseorder.entity.ServiceCaseEntity;
import com.wqst.api.caseorder.entity.SupplementRequestEntity;
import com.wqst.api.caseorder.entity.SupplementRequestItemEntity;
import com.wqst.api.caseorder.mapper.MaterialReviewLogMapper;
import com.wqst.api.caseorder.mapper.MaterialSubmissionMapper;
import com.wqst.api.caseorder.mapper.SupplementRequestItemMapper;
import com.wqst.api.caseorder.mapper.SupplementRequestMapper;
import com.wqst.api.common.BusinessException;
import com.wqst.api.common.IdempotencyService;
import com.wqst.api.company.entity.CompanyUserEntity;
import com.wqst.api.company.mapper.CompanyUserMapper;
import com.wqst.api.notification.NotificationService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {
    private static final Set<String> REVIEW_RESULTS = Set.of("PASSED", "NEED_SUPPLEMENT", "NOT_REQUIRED");
    private final CaseService cases;
    private final MaterialSubmissionMapper materials;
    private final MaterialReviewLogMapper reviewLogs;
    private final SupplementRequestMapper supplements;
    private final SupplementRequestItemMapper supplementItems;
    private final CompanyUserMapper memberships;
    private final NotificationService notifications;
    private final IdempotencyService idempotency;
    private final AuditService audit;

    public ReviewService(CaseService cases, MaterialSubmissionMapper materials, MaterialReviewLogMapper reviewLogs,
                         SupplementRequestMapper supplements, SupplementRequestItemMapper supplementItems,
                         CompanyUserMapper memberships, NotificationService notifications,
                         IdempotencyService idempotency, AuditService audit) {
        this.cases=cases; this.materials=materials; this.reviewLogs=reviewLogs; this.supplements=supplements;
        this.supplementItems=supplementItems; this.memberships=memberships; this.notifications=notifications;
        this.idempotency=idempotency; this.audit=audit;
    }

    @Transactional
    public CaseService.MaterialView review(long materialId, ReviewCommand command, long operatorId) {
        if (!REVIEW_RESULTS.contains(command.reviewStatus())) throw new BusinessException("REVIEW_STATUS_INVALID", "审核结果无效", HttpStatus.BAD_REQUEST);
        MaterialSubmissionEntity m=cases.requireMaterial(materialId);
        ServiceCaseEntity c=cases.require(m.getCaseId());
        if (!List.of("PENDING_REVIEW","PROCESSING","NEED_SUPPLEMENT").contains(c.getStatus()))
            throw new BusinessException("CASE_REVIEW_DENIED", "当前报单状态不允许审核", HttpStatus.CONFLICT);
        if (m.getVersion()==null || !Objects.equals(m.getVersion(),command.version()))
            throw new BusinessException("OPTIMISTIC_LOCK_CONFLICT", "资料已被其他用户修改", HttpStatus.CONFLICT);
        String before=m.getReviewStatus(); m.setReviewStatus(command.reviewStatus()); m.setClientVisibleNote(trim(command.clientVisibleNote()));
        m.setInternalNote(trim(command.internalNote())); m.setReviewedBy(operatorId); m.setReviewedAt(LocalDateTime.now());
        if ("NOT_REQUIRED".equals(command.reviewStatus())) m.setSubmitStatus("SUBMITTED");
        if(materials.updateById(m)==0)throw new BusinessException("OPTIMISTIC_LOCK_CONFLICT","资料已被其他用户修改",HttpStatus.CONFLICT);
        MaterialReviewLogEntity log=new MaterialReviewLogEntity();log.setMaterialId(m.getId());log.setCaseId(m.getCaseId());log.setBeforeStatus(before);
        log.setAfterStatus(m.getReviewStatus());log.setClientVisibleNote(m.getClientVisibleNote());log.setInternalNote(m.getInternalNote());log.setReviewedBy(operatorId);reviewLogs.insert(log);
        audit.operation(operatorId,"MATERIAL_REVIEW","MATERIAL",materialId,"资料审核结果 "+command.reviewStatus());
        return new CaseService.MaterialView(m.getId(),m.getMaterialCode(),m.getMaterialName(),m.getInputType(),Boolean.TRUE.equals(m.getRequired()),Boolean.TRUE.equals(m.getIsSensitive()),m.getTextValue(),m.getSubmitStatus(),m.getReviewStatus(),m.getClientVisibleNote(),m.getInternalNote(),m.getVersion());
    }

    public SupplementView requestSupplement(long caseId, SupplementCommand command, long operatorId, String key) {
        return idempotency.execute(Long.toString(operatorId),"supplement-"+caseId,key,new TypeReference<SupplementView>(){},
                ()->requestTransaction(caseId,command,operatorId));
    }

    protected SupplementView requestTransaction(long caseId,SupplementCommand command,long operatorId){
        ServiceCaseEntity c=cases.require(caseId);
        if(command.materialIds()==null||command.materialIds().isEmpty())throw new BusinessException("SUPPLEMENT_ITEMS_REQUIRED","请选择需要补充的资料项",HttpStatus.BAD_REQUEST);
        if(new HashSet<>(command.materialIds()).size()!=command.materialIds().size())throw new BusinessException("SUPPLEMENT_ITEMS_DUPLICATE","补件资料项不能重复",HttpStatus.BAD_REQUEST);
        for(Long id:command.materialIds()){MaterialSubmissionEntity m=cases.requireMaterial(id);if(!Objects.equals(m.getCaseId(),caseId))throw new BusinessException("MATERIAL_CASE_MISMATCH","补件资料项不属于该报单",HttpStatus.BAD_REQUEST);m.setReviewStatus("NEED_SUPPLEMENT");m.setSubmitStatus("NOT_SUBMITTED");m.setClientVisibleNote(command.clientMessage());if(materials.updateById(m)==0)throw new BusinessException("OPTIMISTIC_LOCK_CONFLICT","资料已被其他用户修改",HttpStatus.CONFLICT);}
        SupplementRequestEntity request=new SupplementRequestEntity();request.setCaseId(caseId);request.setRequestNo(nextNo());request.setClientMessage(command.clientMessage().trim());request.setStatus("OPEN");request.setRequestedBy(operatorId);supplements.insert(request);
        for(Long materialId:command.materialIds()){SupplementRequestItemEntity item=new SupplementRequestItemEntity();item.setRequestId(request.getId());item.setMaterialId(materialId);supplementItems.insert(item);}
        cases.adminTransition(caseId,"NEED_SUPPLEMENT",command.clientMessage().trim(),operatorId);
        for(CompanyUserEntity member:memberships.findEnabledByCompany(c.getCompanyId()))notifications.send(member.getUserId(),c.getCompanyId(),caseId,"SUPPLEMENT_REQUIRED","报单需要补充资料",command.clientMessage().trim());
        audit.operation(operatorId,"SUPPLEMENT_REQUEST","CASE",caseId,"发起补件，资料项数 "+command.materialIds().size());
        return new SupplementView(request.getId(),request.getRequestNo(),caseId,"OPEN",command.materialIds(),request.getClientMessage());
    }

    @Transactional
    public void resolveOpen(long caseId){List<SupplementRequestEntity> open=supplements.selectList(new LambdaQueryWrapper<SupplementRequestEntity>().eq(SupplementRequestEntity::getCaseId,caseId).eq(SupplementRequestEntity::getStatus,"OPEN"));for(SupplementRequestEntity r:open){r.setStatus("RESOLVED");r.setResolvedAt(LocalDateTime.now());supplements.updateById(r);}}
    private String nextNo(){return "BJ"+LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))+UUID.randomUUID().toString().substring(0,4).toUpperCase();}
    private String trim(String value){return value==null?null:value.trim();}
    public record ReviewCommand(String reviewStatus,String clientVisibleNote,String internalNote,Integer version){}
    public record SupplementCommand(List<Long> materialIds,String clientMessage){}
    public record SupplementView(Long id,String requestNo,Long caseId,String status,List<Long> materialIds,String clientMessage){}
}
