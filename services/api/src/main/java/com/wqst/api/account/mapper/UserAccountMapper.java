package com.wqst.api.account.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wqst.api.account.entity.UserAccountEntity;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;

@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccountEntity> {
    @Select("SELECT * FROM user_account WHERE username=#{username} AND deleted_at IS NULL LIMIT 1")
    UserAccountEntity findByUsername(String username);

    @Select("SELECT r.role_code FROM role r JOIN user_role ur ON ur.role_id=r.id WHERE ur.user_id=#{userId} AND r.status='ENABLED'")
    List<String> findRoles(long userId);

    @Select("SELECT DISTINCT p.permission_code FROM permission p JOIN role_permission rp ON rp.permission_id=p.id JOIN user_role ur ON ur.role_id=rp.role_id WHERE ur.user_id=#{userId}")
    List<String> findPermissions(long userId);

    @Update("UPDATE user_account SET last_login_at=NOW(3) WHERE id=#{userId}")
    int touchLogin(long userId);

    @Insert("INSERT IGNORE INTO user_role(user_id,role_id) SELECT #{userId},id FROM role WHERE role_code=#{roleCode}")
    int assignRole(@Param("userId") long userId, @Param("roleCode") String roleCode);

    @Insert("INSERT IGNORE INTO role_permission(role_id,permission_id) SELECT r.id,p.id FROM role r CROSS JOIN permission p WHERE r.role_code='SUPER_ADMIN'")
    int grantAllToSuperAdmin();

    @Delete("DELETE FROM user_role WHERE user_id=#{userId}")
    int clearRoles(long userId);

    @Select("SELECT COUNT(1) FROM role WHERE role_code=#{roleCode} AND status='ENABLED'")
    int roleExists(String roleCode);
}
