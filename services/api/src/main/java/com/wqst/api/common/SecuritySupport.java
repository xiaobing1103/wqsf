package com.wqst.api.common;

import cn.dev33.satoken.stp.StpUtil;
import com.wqst.api.auth.AuthService;
import org.springframework.stereotype.Component;

@Component
public class SecuritySupport {
    public long adminId() {
        StpUtil.checkLogin();
        return StpUtil.getLoginIdAsLong();
    }

    public long clientId() {
        AuthService.CLIENT_LOGIC.checkLogin();
        return AuthService.CLIENT_LOGIC.getLoginIdAsLong();
    }

    public void permission(String code) {
        StpUtil.checkPermission(code);
    }

    public void clientPermission(String code) {
        AuthService.CLIENT_LOGIC.checkPermission(code);
    }

    public boolean isAdmin() {
        return StpUtil.isLogin();
    }
}
