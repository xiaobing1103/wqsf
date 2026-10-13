package com.wqst.api.company.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wqst.api.company.entity.CompanyUserEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CompanyUserMapper extends BaseMapper<CompanyUserEntity> {
    @Select("SELECT * FROM company_user WHERE user_id=#{userId} AND status='ENABLED'")
    List<CompanyUserEntity> findEnabledByUser(long userId);

    @Select("SELECT * FROM company_user WHERE company_id=#{companyId} AND status='ENABLED'")
    List<CompanyUserEntity> findEnabledByCompany(long companyId);

    @Select("SELECT COUNT(1) FROM company_user WHERE company_id=#{companyId} AND user_id=#{userId} AND status='ENABLED'")
    int hasAccess(@Param("companyId") long companyId, @Param("userId") long userId);

    @Update("UPDATE company_user SET status='DISABLED', updated_at=NOW(3) WHERE company_id=#{companyId} AND status<>'DISABLED'")
    int disableByCompanyId(long companyId);
}
