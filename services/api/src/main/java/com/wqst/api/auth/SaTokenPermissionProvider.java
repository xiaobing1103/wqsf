package com.wqst.api.auth;

import cn.dev33.satoken.stp.StpInterface;
import com.wqst.api.account.mapper.UserAccountMapper;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SaTokenPermissionProvider implements StpInterface {
    private final UserAccountMapper users;
    public SaTokenPermissionProvider(UserAccountMapper users) { this.users = users; }
    @Override public List<String> getPermissionList(Object loginId, String loginType) {
        return isSupported(loginType) ? users.findPermissions(Long.parseLong(loginId.toString())) : List.of();
    }
    @Override public List<String> getRoleList(Object loginId, String loginType) {
        return isSupported(loginType) ? users.findRoles(Long.parseLong(loginId.toString())) : List.of();
    }
    private boolean isSupported(String loginType) { return "login".equals(loginType) || "client".equals(loginType); }
}
