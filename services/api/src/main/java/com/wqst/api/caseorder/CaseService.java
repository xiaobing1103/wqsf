package com.wqst.api.caseorder;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.wqst.api.audit.AuditService;
import com.wqst.api.account.entity.UserAccountEntity;
import com.wqst.api.account.mapper.UserAccountMapper;
import com.wqst.api.caseorder.entity.CaseStatusLogEntity;
import com.wqst.api.caseorder.entity.InvoiceInfoEntity;
import com.wqst.api.caseorder.entity.MaterialSubmissionEntity;
import com.wqst.api.caseorder.entity.ServiceCaseEntity;
import com.wqst.api.caseorder.entity.SupplementRequestEntity;
import com.wqst.api.caseorder.mapper.CaseStatusLogMapper;
import com.wqst.api.caseorder.mapper.InvoiceInfoMapper;
import com.wqst.api.caseorder.mapper.MaterialSubmissionMapper;
import com.wqst.api.caseorder.mapper.ServiceCaseMapper;
import com.wqst.api.caseorder.mapper.SupplementRequestMapper;
import com.wqst.api.catalog.CatalogService;
import com.wqst.api.catalog.entity.MaterialTemplateItemEntity;
import com.wqst.api.catalog.entity.ServiceProductEntity;
import com.wqst.api.common.BusinessException;
import com.wqst.api.common.CaseStateMachine;
import com.wqst.api.common.CryptoService;
import com.wqst.api.common.IdempotencyService;
import com.wqst.api.common.PageResponse;
import com.wqst.api.company.CompanyService;
import com.wqst.api.company.entity.CompanyEntity;
import com.wqst.api.company.entity.CompanyUserEntity;
import com.wqst.api.company.mapper.CompanyUserMapper;
import com.wqst.api.notification.NotificationService;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CaseService {
    private final ServiceCaseMapper cases;
    private final MaterialSubmissionMapper materials;
    private final InvoiceInfoMapper invoices;
    private final CaseStatusLogMapper statusLogs;
    private final SupplementRequestMapper supplements;
    private final CompanyUserMapper memberships;
    private final CompanyService companies;
    private final CatalogService catalog;
    private final CryptoService crypto;
    private final CaseStateMachine stateMachine;
    private final IdempotencyService idempotency;
    private final AuditService audit;
    private final UserAccountMapper users;
    private final NotificationService notifications;

    public CaseService(ServiceCaseMapper cases, MaterialSubmissionMapper materials, InvoiceInfoMapper invoices,
                       CaseStatusLogMapper statusLogs, SupplementRequestMapper supplements, CompanyUserMapper memberships, CompanyService companies,
                       CatalogService catalog, CryptoService crypto, CaseStateMachine stateMachine,
                       IdempotencyService idempotency, AuditService audit, UserAccountMapper users, NotificationService notifications) {
        this.cases=cases; this.materials=materials; this.invoices=invoices; this.statusLogs=statusLogs; this.supplements=supplements;
        this.memberships=memberships; this.companies=companies; this.catalog=catalog; this.crypto=crypto;
        this.stateMachine=stateMachine; this.idempotency=idempotency; this.audit=audit; this.users=users; this.notifications=notifications;
    }

    public CaseView create(CreateCase command, long userId, String idempotencyKey) {
        return idempotency.execute(Long.toString(userId), "create-case", idempotencyKey, new TypeReference<CaseView>() {},
                () -> createTransaction(command, userId));
    }

    @Transactional
    protected CaseView createTransaction(CreateCase command, long userId) {
        companies.requireClientAccess(command.companyId(), userId);
        CompanyEntity company = companies.require(command.companyId());
        ServiceProductEntity product = catalog.requireProduct(command.productId());
        if (!"PUBLISHED".equals(product.getStatus())) throw new BusinessException("PRODUCT_NOT_PUBLISHED", "产品暂不可报单", HttpStatus.CONFLICT);
        String expectedType = typeForTemplate(catalog.requireTemplate(product.getTemplateId()).getTemplateCode());
        if (!expectedType.equals(command.serviceType())) throw new BusinessException("SERVICE_PRODUCT_MISMATCH", "服务类型与产品不匹配", HttpStatus.BAD_REQUEST);
        String month = null;
        if ("TRADE_INCREMENT".equals(command.serviceType())) {
            try { month = YearMonth.parse(command.caseMonth()).toString(); }
            catch (RuntimeException ex) { throw new BusinessException("CASE_MONTH_INVALID", "贸易增量报单月份格式应为 YYYY-MM", HttpStatus.BAD_REQUEST); }
        }
        ServiceCaseEntity entity = new ServiceCaseEntity();
        entity.setCaseNo(nextCaseNo()); entity.setCompanyId(company.getId()); entity.setProductId(product.getId());
        entity.setServiceType(command.serviceType()); entity.setStatus("DRAFT"); entity.setApplicantUserId(userId);
        entity.setCompanyNameSnapshot(company.getCompanyName()); entity.setCaseMonth(month);
        cases.insert(entity);
        for (MaterialTemplateItemEntity templateItem : catalog.templateItems(product.getTemplateId())) {
            MaterialSubmissionEntity material = new MaterialSubmissionEntity();
            material.setCaseId(entity.getId()); material.setTemplateItemId(templateItem.getId()); material.setMaterialCode(templateItem.getItemCode());
            material.setMaterialName(templateItem.getItemName()); material.setInputType(templateItem.getInputType());
            material.setRequired(templateItem.getIsRequired()); material.setIsSensitive(templateItem.getIsSensitive());
            material.setSubmitStatus("NOT_SUBMITTED"); material.setReviewStatus("PENDING"); materials.insert(material);
        }
        logStatus(entity.getId(), null, "DRAFT", "创建报单草稿", userId);
        audit.operation(userId, "CASE_CREATE", "CASE", entity.getId(), "创建报单草稿 " + entity.getCaseNo());
        return view(entity, false);
    }

    public PageResponse<CaseSummary> mine(long userId, int page, int size, String status) {
        List<Long> companyIds = memberships.findEnabledByUser(userId).stream().map(CompanyUserEntity::getCompanyId).distinct().toList();
        if (companyIds.isEmpty()) return PageResponse.of(List.of(), page, size, 0);
        LambdaQueryWrapper<ServiceCaseEntity> q = new LambdaQueryWrapper<ServiceCaseEntity>()
                .in(ServiceCaseEntity::getCompanyId, companyIds).isNull(ServiceCaseEntity::getDeletedAt)
                .eq(StringUtils.hasText(status), ServiceCaseEntity::getStatus, status)
                .orderByDesc(ServiceCaseEntity::getCreatedAt).orderByDesc(ServiceCaseEntity::getId);
        Page<ServiceCaseEntity> result = cases.selectPage(Page.of(page, size), q);
        return PageResponse.of(result.getRecords().stream().map(this::summary).toList(), page, size, result.getTotal());
    }

    public CaseView clientDetail(long id, long userId) {
        ServiceCaseEntity c = require(id); companies.requireClientAccess(c.getCompanyId(), userId); return view(c, false);
    }

    public CaseView adminDetail(long id) { return view(require(id), true); }

    @Transactional
    public InvoiceView saveInvoice(long caseId, InvoiceCommand command, long userId) {
        ServiceCaseEntity c = requireEditable(caseId, userId);
        if (!"TRADE_INCREMENT".equals(c.getServiceType())) throw new BusinessException("INVOICE_NOT_APPLICABLE", "该报单不需要开票信息", HttpStatus.CONFLICT);
        InvoiceInfoEntity invoice = invoices.selectOne(new LambdaQueryWrapper<InvoiceInfoEntity>().eq(InvoiceInfoEntity::getCaseId, caseId));
        if (invoice == null) { invoice = new InvoiceInfoEntity(); invoice.setCaseId(caseId); }
        invoice.setCompanyName(command.companyName().trim()); invoice.setTaxNoCipher(crypto.encrypt(command.taxNo())); invoice.setTaxNoMask(crypto.maskTaxNo(command.taxNo()));
        invoice.setContactAddress(command.contactAddress().trim()); invoice.setContactPhoneCipher(crypto.encrypt(command.contactPhone()));
        invoice.setContactPhoneHash(crypto.hash(command.contactPhone())); invoice.setContactPhoneMask(crypto.maskPhone(command.contactPhone()));
        invoice.setLegalPerson(command.legalPerson().trim()); invoice.setCompanyEmail(command.companyEmail().trim()); invoice.setBankBranch(command.bankBranch().trim());
        invoice.setBasicAccountCipher(crypto.encrypt(command.basicAccount())); invoice.setBasicAccountMask(crypto.maskAccount(command.basicAccount()));
        if (invoice.getId() == null) invoices.insert(invoice); else invoices.updateById(invoice);
        markSubmitted(caseId, "INVOICE_INFO", null);
        audit.operation(userId, "INVOICE_SAVE", "CASE", caseId, "保存开票信息（敏感字段已加密）");
        return invoiceView(invoice);
    }

    @Transactional
    public CaseView saveSimple(long caseId, SimpleCommand command, long userId) {
        ServiceCaseEntity c = requireEditable(caseId, userId);
        if ("TRADE_INCREMENT".equals(c.getServiceType())) throw new BusinessException("SIMPLE_FORM_NOT_APPLICABLE", "贸易增量应填写完整资料表", HttpStatus.CONFLICT);
        if (!command.consent()) throw new BusinessException("CONSENT_REQUIRED", "请确认资料使用授权", HttpStatus.BAD_REQUEST);
        c.setLegalPersonName(command.legalPersonName().trim()); c.setCompanyNameSnapshot(command.companyName().trim());
        c.setContactPhoneCipher(crypto.encrypt(command.contactPhone())); c.setContactPhoneHash(crypto.hash(command.contactPhone()));
        c.setContactPhoneMask(crypto.maskPhone(command.contactPhone()));
        if (cases.updateById(c) == 0) throw optimistic();
        markSubmitted(caseId, "LEGAL_PERSON_NAME", command.legalPersonName().trim());
        markSubmitted(caseId, "COMPANY_NAME", command.companyName().trim());
        markSubmitted(caseId, "CONTACT_PHONE", crypto.maskPhone(command.contactPhone()));
        audit.operation(userId, "SIMPLE_FORM_SAVE", "CASE", caseId, "保存简表单");
        return view(cases.selectById(caseId), false);
    }

    @Transactional
    public MaterialView saveText(long caseId, long materialId, String value, long userId) {
        requireEditable(caseId, userId);
        MaterialSubmissionEntity m = requireMaterial(materialId);
        if (!Objects.equals(m.getCaseId(), caseId) || !List.of("TEXT", "URL").contains(m.getInputType()))
            throw new BusinessException("MATERIAL_INPUT_INVALID", "该资料项不支持文字输入", HttpStatus.BAD_REQUEST);
        String text = value == null ? "" : value.trim();
        if (text.isBlank()) throw new BusinessException("MATERIAL_VALUE_REQUIRED", "资料内容不能为空", HttpStatus.BAD_REQUEST);
        if ("URL".equals(m.getInputType())) {
            try { URI uri=URI.create(text); if (!List.of("http","https").contains(uri.getScheme().toLowerCase(Locale.ROOT))) throw new IllegalArgumentException(); }
            catch (RuntimeException ex) { throw new BusinessException("MATERIAL_URL_INVALID", "请输入有效的 http/https 链接", HttpStatus.BAD_REQUEST); }
        }
        m.setTextValue(text); m.setSubmitStatus("SUBMITTED"); m.setReviewStatus("PENDING"); m.setClientVisibleNote(null); m.setInternalNote(null);
        if (materials.updateById(m) == 0) throw optimistic();
        return materialView(m, false);
    }

    public CaseView submit(long caseId, long userId, String key) {
        return idempotency.execute(Long.toString(userId), "submit-case-" + caseId, key, new TypeReference<CaseView>() {},
                () -> submitTransaction(caseId, userId));
    }

    @Transactional
    protected CaseView submitTransaction(long caseId, long userId) {
        ServiceCaseEntity c = requireEditable(caseId, userId);
        List<MaterialSubmissionEntity> missing = caseMaterials(caseId).stream()
                .filter(m -> Boolean.TRUE.equals(m.getRequired()) && !"SUBMITTED".equals(m.getSubmitStatus()) && !"NOT_REQUIRED".equals(m.getReviewStatus())).toList();
        if (!missing.isEmpty()) {
            throw new BusinessException("MATERIAL_REQUIRED", "请先补齐必填资料", HttpStatus.BAD_REQUEST,
                    Map.of("missingItems", missing.stream().map(m -> Map.of("materialId", m.getId(), "materialCode", m.getMaterialCode(), "materialName", m.getMaterialName())).toList()));
        }
        transition(c, "PENDING_REVIEW", "客户提交报单", userId);
        supplements.selectList(new LambdaQueryWrapper<SupplementRequestEntity>()
                .eq(SupplementRequestEntity::getCaseId,caseId).eq(SupplementRequestEntity::getStatus,"OPEN"))
                .forEach(r->{r.setStatus("RESOLVED");r.setResolvedAt(LocalDateTime.now());supplements.updateById(r);});
        audit.operation(userId, "CASE_SUBMIT", "CASE", c.getId(), "提交报单 " + c.getCaseNo());
        return view(cases.selectById(caseId), false);
    }

    public PageResponse<CaseSummary> adminList(int page, int size, String keyword, String status, String serviceType, Long assigneeId) {
        LambdaQueryWrapper<ServiceCaseEntity> q = new LambdaQueryWrapper<ServiceCaseEntity>().isNull(ServiceCaseEntity::getDeletedAt)
                .eq(StringUtils.hasText(status), ServiceCaseEntity::getStatus, status)
                .eq(StringUtils.hasText(serviceType), ServiceCaseEntity::getServiceType, serviceType)
                .eq(assigneeId != null, ServiceCaseEntity::getAssigneeId, assigneeId)
                .and(StringUtils.hasText(keyword), w -> w.like(ServiceCaseEntity::getCaseNo, keyword).or().like(ServiceCaseEntity::getCompanyNameSnapshot, keyword))
                .orderByDesc(ServiceCaseEntity::getCreatedAt).orderByDesc(ServiceCaseEntity::getId);
        Page<ServiceCaseEntity> result=cases.selectPage(Page.of(page,size),q);
        return PageResponse.of(result.getRecords().stream().map(this::summary).toList(),page,size,result.getTotal());
    }

    @Transactional
    public CaseView assign(long caseId, long assigneeId, long operatorId) {
        UserAccountEntity assignee=users.selectById(assigneeId);if(assignee==null||!"ADMIN".equals(assignee.getAccountType())||!"ENABLED".equals(assignee.getStatus()))throw new BusinessException("ASSIGNEE_INVALID","负责人不是可用的后台账号",HttpStatus.BAD_REQUEST);
        ServiceCaseEntity c=require(caseId); c.setAssigneeId(assigneeId); if(cases.updateById(c)==0)throw optimistic();
        audit.operation(operatorId,"CASE_ASSIGN","CASE",caseId,"分配报单负责人"); return view(cases.selectById(caseId),true);
    }

    @Transactional
    public CaseView adminTransition(long caseId, String target, String note, long operatorId) {
        ServiceCaseEntity c=require(caseId); transition(c,target,note,operatorId);
        for(CompanyUserEntity member:memberships.findEnabledByCompany(c.getCompanyId()))notifications.send(member.getUserId(),c.getCompanyId(),caseId,"CASE_STATUS_CHANGED","报单状态已更新","报单 "+c.getCaseNo()+" 已更新为 "+target);
        audit.operation(operatorId,"CASE_STATUS_CHANGE","CASE",caseId,"报单状态变更为 "+target); return view(cases.selectById(caseId),true);
    }

    public List<TimelineView> timeline(long caseId,long userId){ServiceCaseEntity c=require(caseId);companies.requireClientAccess(c.getCompanyId(),userId);return statusLogs.selectList(new LambdaQueryWrapper<CaseStatusLogEntity>().eq(CaseStatusLogEntity::getCaseId,caseId).orderByAsc(CaseStatusLogEntity::getCreatedAt)).stream().map(l->new TimelineView(l.getAfterStatus(),clientNote(l.getAfterStatus(),l.getNote()),l.getCreatedAt())).toList();}

    public ServiceCaseEntity require(long id) { ServiceCaseEntity c=cases.selectById(id); if(c==null||c.getDeletedAt()!=null)throw new BusinessException("CASE_NOT_FOUND","报单不存在",HttpStatus.NOT_FOUND);return c; }
    public MaterialSubmissionEntity requireMaterial(long id){MaterialSubmissionEntity m=materials.selectById(id);if(m==null)throw new BusinessException("MATERIAL_NOT_FOUND","资料项不存在",HttpStatus.NOT_FOUND);return m;}
    public List<MaterialSubmissionEntity> caseMaterials(long caseId){return materials.selectList(new LambdaQueryWrapper<MaterialSubmissionEntity>().eq(MaterialSubmissionEntity::getCaseId,caseId).orderByAsc(MaterialSubmissionEntity::getId));}

    @Transactional
    public void markFileSubmitted(long caseId,long materialId){MaterialSubmissionEntity m=requireMaterial(materialId);if(!Objects.equals(m.getCaseId(),caseId)||!"FILE".equals(m.getInputType()))throw new BusinessException("MATERIAL_INPUT_INVALID","文件与资料项不匹配",HttpStatus.BAD_REQUEST);m.setSubmitStatus("SUBMITTED");m.setReviewStatus("PENDING");m.setClientVisibleNote(null);m.setInternalNote(null);if(materials.updateById(m)==0)throw optimistic();}

    private ServiceCaseEntity requireEditable(long id,long userId){ServiceCaseEntity c=require(id);companies.requireClientAccess(c.getCompanyId(),userId);if(!List.of("DRAFT","NEED_SUPPLEMENT").contains(c.getStatus()))throw new BusinessException("CASE_NOT_EDITABLE","当前状态不允许修改资料",HttpStatus.CONFLICT);return c;}
    private void transition(ServiceCaseEntity c,String target,String note,long operatorId){String before=c.getStatus();stateMachine.check(before,target);c.setStatus(target);if("PENDING_REVIEW".equals(target))c.setSubmittedAt(LocalDateTime.now());if("COMPLETED".equals(target))c.setCompletedAt(LocalDateTime.now());if(cases.updateById(c)==0)throw optimistic();logStatus(c.getId(),before,target,note,operatorId);}
    private void logStatus(long caseId,String before,String after,String note,Long userId){CaseStatusLogEntity l=new CaseStatusLogEntity();l.setCaseId(caseId);l.setBeforeStatus(before);l.setAfterStatus(after);l.setNote(note);l.setChangedBy(userId);statusLogs.insert(l);}
    private void markSubmitted(long caseId,String code,String value){MaterialSubmissionEntity m=materials.selectOne(new LambdaQueryWrapper<MaterialSubmissionEntity>().eq(MaterialSubmissionEntity::getCaseId,caseId).eq(MaterialSubmissionEntity::getMaterialCode,code));if(m==null)throw new BusinessException("MATERIAL_NOT_FOUND","资料项不存在",HttpStatus.CONFLICT);m.setTextValue(value);m.setSubmitStatus("SUBMITTED");m.setReviewStatus("PENDING");m.setClientVisibleNote(null);m.setInternalNote(null);if(materials.updateById(m)==0)throw optimistic();}
    private String typeForTemplate(String code){return switch(code){case "TRADE_INCREMENT"->"TRADE_INCREMENT";case "IP_SIMPLE"->"IP";case "QUALIFICATION_SIMPLE"->"QUALIFICATION";default->throw new BusinessException("TEMPLATE_SERVICE_UNKNOWN","模板服务类型未知",HttpStatus.CONFLICT);};}
    private String nextCaseNo(){return "BD"+LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))+UUID.randomUUID().toString().substring(0,4).toUpperCase(Locale.ROOT);}
    private BusinessException optimistic(){return new BusinessException("OPTIMISTIC_LOCK_CONFLICT","数据已被其他用户修改，请刷新后重试",HttpStatus.CONFLICT);}

    private CaseSummary summary(ServiceCaseEntity c){Progress p=progress(c.getId());return new CaseSummary(c.getId(),c.getCaseNo(),c.getCompanyNameSnapshot(),c.getServiceType(),c.getStatus(),c.getCaseMonth(),p,c.getCreatedAt(),c.getUpdatedAt());}
    private CaseView view(ServiceCaseEntity c,boolean internal){Progress p=progress(c.getId());List<MaterialView> ms=caseMaterials(c.getId()).stream().map(m->materialView(m,internal)).toList();InvoiceInfoEntity invoice=invoices.selectOne(new LambdaQueryWrapper<InvoiceInfoEntity>().eq(InvoiceInfoEntity::getCaseId,c.getId()));return new CaseView(c.getId(),c.getCaseNo(),c.getCompanyId(),c.getProductId(),c.getCompanyNameSnapshot(),c.getServiceType(),c.getStatus(),c.getCaseMonth(),c.getAssigneeId(),c.getApplicantUserId(),c.getLegalPersonName(),c.getContactPhoneMask(),p,ms,invoice==null?null:invoiceView(invoice),c.getSubmittedAt(),c.getCompletedAt(),c.getCreatedAt(),c.getUpdatedAt(),c.getVersion());}
    private Progress progress(long caseId){List<MaterialSubmissionEntity> list=caseMaterials(caseId);long completed=list.stream().filter(m->"SUBMITTED".equals(m.getSubmitStatus())||"NOT_REQUIRED".equals(m.getReviewStatus())).count();long remaining=list.stream().filter(m->Boolean.TRUE.equals(m.getRequired())&&!"SUBMITTED".equals(m.getSubmitStatus())&&!"NOT_REQUIRED".equals(m.getReviewStatus())).count();return new Progress(list.size(),completed,remaining);}
    private MaterialView materialView(MaterialSubmissionEntity m,boolean internal){return new MaterialView(m.getId(),m.getMaterialCode(),m.getMaterialName(),m.getInputType(),Boolean.TRUE.equals(m.getRequired()),Boolean.TRUE.equals(m.getIsSensitive()),m.getTextValue(),m.getSubmitStatus(),m.getReviewStatus(),m.getClientVisibleNote(),internal?m.getInternalNote():null,m.getVersion());}
    private InvoiceView invoiceView(InvoiceInfoEntity i){return new InvoiceView(i.getCompanyName(),i.getTaxNoMask(),i.getContactAddress(),i.getContactPhoneMask(),i.getLegalPerson(),i.getCompanyEmail(),i.getBankBranch(),i.getBasicAccountMask());}
    private String clientNote(String status,String note){return "NEED_SUPPLEMENT".equals(status)?note:null;}

    public record CreateCase(long companyId,long productId,String serviceType,String caseMonth){}
    public record InvoiceCommand(String companyName,String taxNo,String contactAddress,String contactPhone,String legalPerson,String companyEmail,String bankBranch,String basicAccount){}
    public record SimpleCommand(String legalPersonName,String companyName,String contactPhone,boolean consent){}
    public record Progress(long total,long completed,long requiredRemaining){}
    public record MaterialView(Long id,String materialCode,String materialName,String inputType,boolean required,boolean sensitive,String textValue,String submitStatus,String reviewStatus,String clientVisibleNote,String internalNote,Integer version){}
    public record InvoiceView(String companyName,String taxNoMask,String contactAddress,String contactPhoneMask,String legalPerson,String companyEmail,String bankBranch,String basicAccountMask){}
    public record CaseSummary(Long id,String caseNo,String companyName,String serviceType,String status,String caseMonth,Progress materials,LocalDateTime createdAt,LocalDateTime updatedAt){}
    public record CaseView(Long id,String caseNo,Long companyId,Long productId,String companyName,String serviceType,String status,String caseMonth,Long assigneeId,Long applicantUserId,String legalPersonName,String contactPhoneMask,Progress progress,List<MaterialView> materials,InvoiceView invoiceInfo,LocalDateTime submittedAt,LocalDateTime completedAt,LocalDateTime createdAt,LocalDateTime updatedAt,Integer version){}
    public record TimelineView(String status,String note,LocalDateTime time){}
}
