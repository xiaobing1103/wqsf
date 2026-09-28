package com.wqst.api.auth;

import com.wqst.api.account.entity.UserAccountEntity;
import com.wqst.api.account.mapper.UserAccountMapper;
import com.wqst.api.config.AppProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class BootstrapAdminRunner implements ApplicationRunner {
    private final UserAccountMapper users; private final PasswordEncoder passwords; private final AppProperties props;
    public BootstrapAdminRunner(UserAccountMapper users, PasswordEncoder passwords, AppProperties props) { this.users=users; this.passwords=passwords; this.props=props; }
    @Override public void run(ApplicationArguments args) {
        users.grantAllToSuperAdmin();
        String username=props.bootstrap().adminUsername(), password=props.bootstrap().adminPassword();
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password) || users.findByUsername(username) != null) return;
        if (password.length() < 12) throw new IllegalStateException("BOOTSTRAP_ADMIN_PASSWORD 至少需要 12 位");
        UserAccountEntity user=new UserAccountEntity(); user.setAccountType("ADMIN"); user.setUsername(username); user.setDisplayName("系统管理员");
        user.setPasswordHash(passwords.encode(password)); user.setStatus("ENABLED"); users.insert(user);
        users.assignRole(user.getId(), "SUPER_ADMIN");
    }
}
