package com.wqst.api.catalog;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wqst.api.catalog.entity.MaterialTemplateEntity;
import com.wqst.api.catalog.entity.MaterialTemplateItemEntity;
import com.wqst.api.catalog.entity.ServiceModuleEntity;
import com.wqst.api.catalog.entity.ServiceProductEntity;
import com.wqst.api.catalog.mapper.MaterialTemplateItemMapper;
import com.wqst.api.catalog.mapper.MaterialTemplateMapper;
import com.wqst.api.catalog.mapper.ServiceModuleMapper;
import com.wqst.api.catalog.mapper.ServiceProductMapper;
import com.wqst.api.common.BusinessException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogService {
    private final ServiceModuleMapper modules;
    private final ServiceProductMapper products;
    private final MaterialTemplateMapper templates;
    private final MaterialTemplateItemMapper items;

    public CatalogService(ServiceModuleMapper modules, ServiceProductMapper products, MaterialTemplateMapper templates,
                          MaterialTemplateItemMapper items) {
        this.modules = modules; this.products = products; this.templates = templates; this.items = items;
    }

    public List<ModuleView> catalog() {
        List<ServiceModuleEntity> enabled = modules.selectList(new LambdaQueryWrapper<ServiceModuleEntity>()
                .eq(ServiceModuleEntity::getStatus, "ENABLED").orderByAsc(ServiceModuleEntity::getSortOrder));
        return enabled.stream().map(m -> new ModuleView(m.getModuleCode(), m.getModuleName(), m.getDescription(),
                products.selectList(new LambdaQueryWrapper<ServiceProductEntity>()
                                .eq(ServiceProductEntity::getModuleId, m.getId()).eq(ServiceProductEntity::getStatus, "PUBLISHED")
                                .isNull(ServiceProductEntity::getDeletedAt).orderByAsc(ServiceProductEntity::getSortOrder))
                        .stream().map(this::productView).toList())).toList();
    }

    public TemplateView templateForProduct(long productId) {
        ServiceProductEntity product = requireProduct(productId);
        MaterialTemplateEntity template = templates.selectById(product.getTemplateId());
        if (template == null || !"PUBLISHED".equals(template.getStatus()))
            throw new BusinessException("TEMPLATE_NOT_PUBLISHED", "产品资料模板尚未发布", HttpStatus.CONFLICT);
        List<ItemView> materialItems = templateItems(template.getId()).stream().map(this::itemView).toList();
        return new TemplateView(template.getId(), template.getTemplateCode(), template.getTemplateName(), template.getVersionNo(), materialItems);
    }

    public List<ProductAdminView> adminProducts() {
        return products.selectList(new LambdaQueryWrapper<ServiceProductEntity>().isNull(ServiceProductEntity::getDeletedAt)
                .orderByAsc(ServiceProductEntity::getSortOrder)).stream().map(p -> new ProductAdminView(p.getId(), p.getModuleId(),
                p.getProductCode(), p.getProductName(), p.getDescription(), p.getTemplateId(), p.getSortOrder(), p.getStatus(), p.getVersion())).toList();
    }

    @Transactional
    public ProductAdminView create(ProductCommand command, long userId) {
        requireTemplate(command.templateId());
        if (modules.selectById(command.moduleId()) == null) throw new BusinessException("MODULE_NOT_FOUND", "服务模块不存在", HttpStatus.NOT_FOUND);
        if (products.selectCount(new LambdaQueryWrapper<ServiceProductEntity>().eq(ServiceProductEntity::getProductCode, command.productCode())) > 0)
            throw new BusinessException("PRODUCT_CODE_EXISTS", "产品编码已存在", HttpStatus.CONFLICT);
        ServiceProductEntity p = new ServiceProductEntity();
        p.setModuleId(command.moduleId()); p.setProductCode(command.productCode().trim()); p.setProductName(command.productName().trim());
        p.setDescription(command.description()); p.setTemplateId(command.templateId()); p.setSortOrder(command.sortOrder());
        p.setStatus("DRAFT"); p.setCreatedBy(userId); products.insert(p);
        return adminView(p);
    }

    @Transactional
    public ProductAdminView update(long productId, ProductCommand command, int version) {
        ServiceProductEntity p = requireProduct(productId);
        if (!"DRAFT".equals(p.getStatus()) && !"UNPUBLISHED".equals(p.getStatus()))
            throw new BusinessException("PRODUCT_EDIT_DENIED", "已发布产品需先下架后修改", HttpStatus.CONFLICT);
        requireTemplate(command.templateId());
        p.setModuleId(command.moduleId()); p.setProductName(command.productName().trim()); p.setDescription(command.description());
        p.setTemplateId(command.templateId()); p.setSortOrder(command.sortOrder()); p.setVersion(version);
        if (products.updateById(p) == 0) throw new BusinessException("OPTIMISTIC_LOCK_CONFLICT", "产品已被其他用户修改", HttpStatus.CONFLICT);
        return adminView(p);
    }

    @Transactional
    public ProductAdminView status(long productId, String status) {
        ServiceProductEntity p = requireProduct(productId);
        if (!List.of("PUBLISHED", "UNPUBLISHED").contains(status)) throw new IllegalArgumentException("产品状态无效");
        MaterialTemplateEntity t = requireTemplate(p.getTemplateId());
        if ("PUBLISHED".equals(status) && !"PUBLISHED".equals(t.getStatus()))
            throw new BusinessException("TEMPLATE_NOT_PUBLISHED", "资料模板未发布", HttpStatus.CONFLICT);
        p.setStatus(status); products.updateById(p); return adminView(p);
    }

    @Transactional
    public TemplateView createTemplateVersion(long sourceTemplateId, String name, List<TemplateItemCommand> commands, long userId) {
        MaterialTemplateEntity source = requireTemplate(sourceTemplateId);
        Integer max = templates.selectList(new LambdaQueryWrapper<MaterialTemplateEntity>().eq(MaterialTemplateEntity::getTemplateCode, source.getTemplateCode()))
                .stream().map(MaterialTemplateEntity::getVersionNo).max(Integer::compareTo).orElse(0);
        MaterialTemplateEntity t = new MaterialTemplateEntity();
        t.setTemplateCode(source.getTemplateCode()); t.setTemplateName(name); t.setVersionNo(max + 1); t.setStatus("DRAFT"); t.setCreatedBy(userId);
        templates.insert(t);
        int order = 1;
        for (TemplateItemCommand c : commands) {
            MaterialTemplateItemEntity item = new MaterialTemplateItemEntity();
            item.setTemplateId(t.getId()); item.setItemCode(c.itemCode()); item.setItemName(c.itemName()); item.setInputType(c.inputType());
            item.setIsRequired(c.required()); item.setIsSensitive(c.sensitive()); item.setAllowedExtensions(c.allowedExtensions());
            item.setMaxFileSizeMb(c.maxFileSizeMb()); item.setMaxFileCount(c.maxFileCount()); item.setHelpText(c.helpText()); item.setSortOrder(order++);
            items.insert(item);
        }
        return new TemplateView(t.getId(), t.getTemplateCode(), t.getTemplateName(), t.getVersionNo(), templateItems(t.getId()).stream().map(this::itemView).toList());
    }

    @Transactional
    public TemplateView publishTemplate(long templateId) {
        MaterialTemplateEntity t = requireTemplate(templateId);
        if (templateItems(templateId).isEmpty()) throw new BusinessException("TEMPLATE_EMPTY", "资料模板不能为空", HttpStatus.CONFLICT);
        t.setStatus("PUBLISHED"); t.setPublishedAt(LocalDateTime.now()); templates.updateById(t);
        return new TemplateView(t.getId(), t.getTemplateCode(), t.getTemplateName(), t.getVersionNo(), templateItems(t.getId()).stream().map(this::itemView).toList());
    }

    public ServiceProductEntity requireProduct(long id) {
        ServiceProductEntity p = products.selectById(id);
        if (p == null || p.getDeletedAt() != null) throw new BusinessException("PRODUCT_NOT_FOUND", "产品不存在", HttpStatus.NOT_FOUND);
        return p;
    }
    public MaterialTemplateEntity requireTemplate(long id) {
        MaterialTemplateEntity t = templates.selectById(id);
        if (t == null) throw new BusinessException("TEMPLATE_NOT_FOUND", "资料模板不存在", HttpStatus.NOT_FOUND);
        return t;
    }
    public List<MaterialTemplateItemEntity> templateItems(long templateId) {
        return items.selectList(new LambdaQueryWrapper<MaterialTemplateItemEntity>().eq(MaterialTemplateItemEntity::getTemplateId, templateId)
                .orderByAsc(MaterialTemplateItemEntity::getSortOrder));
    }

    private ProductView productView(ServiceProductEntity p) {
        long count = items.selectCount(new LambdaQueryWrapper<MaterialTemplateItemEntity>().eq(MaterialTemplateItemEntity::getTemplateId, p.getTemplateId()));
        return new ProductView(p.getId(), p.getProductCode(), p.getProductName(), p.getDescription(), count);
    }
    private ItemView itemView(MaterialTemplateItemEntity i) {
        List<String> extensions = i.getAllowedExtensions() == null ? List.of() : Arrays.stream(i.getAllowedExtensions().split(",")).toList();
        return new ItemView(i.getId(), i.getItemCode(), i.getItemName(), i.getInputType(), Boolean.TRUE.equals(i.getIsRequired()),
                Boolean.TRUE.equals(i.getIsSensitive()), extensions, i.getMaxFileSizeMb(), i.getMaxFileCount(), i.getHelpText(), i.getSortOrder());
    }
    private ProductAdminView adminView(ServiceProductEntity p) { return new ProductAdminView(p.getId(), p.getModuleId(), p.getProductCode(),
            p.getProductName(), p.getDescription(), p.getTemplateId(), p.getSortOrder(), p.getStatus(), p.getVersion()); }

    public record ModuleView(String moduleCode, String moduleName, String description, List<ProductView> products) {}
    public record ProductView(Long id, String productCode, String productName, String description, long materialCount) {}
    public record TemplateView(Long id, String templateCode, String templateName, Integer versionNo, List<ItemView> items) {}
    public record ItemView(Long id, String itemCode, String itemName, String inputType, boolean required, boolean sensitive,
                           List<String> allowedExtensions, Integer maxFileSizeMb, Integer maxFileCount, String helpText, Integer sortOrder) {}
    public record ProductAdminView(Long id, Long moduleId, String productCode, String productName, String description,
                                   Long templateId, Integer sortOrder, String status, Integer version) {}
    public record ProductCommand(Long moduleId, String productCode, String productName, String description, Long templateId, Integer sortOrder) {}
    public record TemplateItemCommand(String itemCode, String itemName, String inputType, boolean required, boolean sensitive,
                                      String allowedExtensions, Integer maxFileSizeMb, Integer maxFileCount, String helpText) {}
}
