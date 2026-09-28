package com.wqst.api.company;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wqst.api.common.BusinessException;
import com.wqst.api.common.CryptoService;
import com.wqst.api.common.PageResponse;
import com.wqst.api.company.entity.CompanyEntity;
import com.wqst.api.company.entity.CompanyUserEntity;
import com.wqst.api.company.mapper.CompanyMapper;
import com.wqst.api.company.mapper.CompanyUserMapper;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CompanyService {
    private final CompanyMapper companies;
    private final CompanyUserMapper memberships;
    private final CryptoService crypto;

    public CompanyService(CompanyMapper companies, CompanyUserMapper memberships, CryptoService crypto) {
        this.companies = companies;
        this.memberships = memberships;
        this.crypto = crypto;
    }

    public List<CompanyView> mine(long userId) {
        return memberships.findEnabledByUser(userId).stream().map(m -> {
            CompanyEntity c = require(m.getCompanyId());
            return view(c, m.getMemberRole());
        }).toList();
    }

    public CompanyView clientDetail(long companyId, long userId) {
        requireClientAccess(companyId, userId);
        String role = memberships.findEnabledByUser(userId).stream()
                .filter(it -> Objects.equals(it.getCompanyId(), companyId)).map(CompanyUserEntity::getMemberRole).findFirst().orElse("VIEWER");
        return view(require(companyId), role);
    }

    public PageResponse<CompanyView> adminList(int page, int pageSize, String keyword, String status) {
        LambdaQueryWrapper<CompanyEntity> q = new LambdaQueryWrapper<CompanyEntity>()
                .isNull(CompanyEntity::getDeletedAt)
                .eq(StringUtils.hasText(status), CompanyEntity::getStatus, status)
                .and(StringUtils.hasText(keyword), w -> w.like(CompanyEntity::getCompanyName, keyword).or().like(CompanyEntity::getCreditCode, keyword))
                .orderByDesc(CompanyEntity::getCreatedAt).orderByDesc(CompanyEntity::getId);
        Page<CompanyEntity> result = companies.selectPage(Page.of(page, pageSize), q);
        return PageResponse.of(result.getRecords().stream().map(c -> view(c, null)).toList(), page, pageSize, result.getTotal());
    }

    @Transactional
    public CompanyView create(CreateCompany command) {
        if (StringUtils.hasText(command.creditCode()) && companies.selectCount(new LambdaQueryWrapper<CompanyEntity>()
                .eq(CompanyEntity::getCreditCode, command.creditCode().trim()).isNull(CompanyEntity::getDeletedAt)) > 0) {
            throw new BusinessException("COMPANY_CREDIT_CODE_EXISTS", "统一社会信用代码已存在", HttpStatus.CONFLICT);
        }
        CompanyEntity c = new CompanyEntity();
        c.setCompanyName(command.companyName().trim());
        c.setCreditCode(trimToNull(command.creditCode()));
        c.setContactName(trimToNull(command.contactName()));
        c.setLegalPerson(trimToNull(command.legalPerson()));
        applyPhone(c, command.contactPhone());
        c.setProvince(trimToNull(command.province()));
        c.setCity(trimToNull(command.city()));
        c.setCompanyEmail(trimToNull(command.companyEmail()));
        c.setDetailAddress(trimToNull(command.detailAddress()));
        c.setStatus("ENABLED");
        companies.insert(c);
        return view(c, null);
    }

    @Transactional
    public void addMember(long companyId, long userId, String memberRole) {
        require(companyId);
        CompanyUserEntity existing = memberships.selectOne(new LambdaQueryWrapper<CompanyUserEntity>()
                .eq(CompanyUserEntity::getCompanyId, companyId).eq(CompanyUserEntity::getUserId, userId));
        if (existing == null) {
            existing = new CompanyUserEntity();
            existing.setCompanyId(companyId);
            existing.setUserId(userId);
            existing.setMemberRole(memberRole);
            existing.setStatus("ENABLED");
            memberships.insert(existing);
        } else {
            existing.setMemberRole(memberRole);
            existing.setStatus("ENABLED");
            memberships.updateById(existing);
        }
    }

    public CompanyEntity require(long id) {
        CompanyEntity c = companies.selectById(id);
        if (c == null || c.getDeletedAt() != null) throw new BusinessException("COMPANY_NOT_FOUND", "企业不存在", HttpStatus.NOT_FOUND);
        return c;
    }

    public void requireClientAccess(long companyId, long userId) {
        if (memberships.hasAccess(companyId, userId) == 0) {
            throw new BusinessException("COMPANY_SCOPE_DENIED", "无权访问该企业数据", HttpStatus.FORBIDDEN);
        }
    }

    private void applyPhone(CompanyEntity c, String phone) {
        c.setContactPhoneCipher(crypto.encrypt(phone));
        c.setContactPhoneHash(crypto.hash(phone));
        c.setContactPhoneMask(crypto.maskPhone(phone));
    }

    private CompanyView view(CompanyEntity c, String memberRole) {
        return new CompanyView(c.getId(), c.getCompanyName(), maskCreditCode(c.getCreditCode()), c.getContactName(),
                c.getContactPhoneMask(), c.getLegalPerson(), c.getProvince(), c.getCity(), c.getCompanyEmail(),
                c.getDetailAddress(), c.getStatus(), memberRole);
    }

    private String maskCreditCode(String value) {
        if (!StringUtils.hasText(value) || value.length() < 6) return value;
        return value.substring(0, 4) + "************" + value.substring(value.length() - 2);
    }

    private String trimToNull(String value) { return StringUtils.hasText(value) ? value.trim() : null; }

    public record CompanyView(Long id, String companyName, String creditCodeMask, String contactName,
                              String contactPhoneMask, String legalPerson, String province, String city,
                              String companyEmail, String detailAddress, String status, String memberRole) {}
    public record CreateCompany(String companyName, String creditCode, String contactName, String contactPhone,
                                String legalPerson, String province, String city, String companyEmail, String detailAddress) {}
}
