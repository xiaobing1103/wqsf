package com.wqst.api.auth;

import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import com.wqst.api.account.entity.UserAccountEntity;
import com.wqst.api.account.mapper.UserAccountMapper;
import com.wqst.api.common.BusinessException;
import com.wqst.api.config.AppProperties;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Service
public class AuthService {
    public static final StpLogic CLIENT_LOGIC = new StpLogic("client");
    private final UserAccountMapper users;
    private final PasswordEncoder passwords;
    private final AppProperties properties;
    private final StringRedisTemplate redis;

    public AuthService(UserAccountMapper users, PasswordEncoder passwords, AppProperties properties, StringRedisTemplate redis) {
        this.users = users; this.passwords = passwords; this.properties = properties; this.redis = redis;
    }

    public LoginResult adminLogin(String username, String password) {
        String normalized = username.trim();
        String failureKey = "wqst:local:auth:fail:" + normalized.toLowerCase();
        try {
            String attempts = redis.opsForValue().get(failureKey);
            if (attempts != null && Integer.parseInt(attempts) >= 5)
                throw new BusinessException("AUTH_RATE_LIMITED", "登录失败次数过多，请稍后再试", HttpStatus.TOO_MANY_REQUESTS);
        } catch (BusinessException ex) { throw ex; } catch (RuntimeException ex) {
            throw new BusinessException("VALKEY_UNAVAILABLE", "登录安全服务暂时不可用", HttpStatus.SERVICE_UNAVAILABLE);
        }
        UserAccountEntity user = users.findByUsername(normalized);
        if (user == null || !"ADMIN".equals(user.getAccountType()) || !"ENABLED".equals(user.getStatus()) ||
                !passwords.matches(password, user.getPasswordHash())) {
            try { Long value = redis.opsForValue().increment(failureKey); if (value != null && value == 1) redis.expire(failureKey, Duration.ofMinutes(15)); }
            catch (RuntimeException ex) { throw new BusinessException("VALKEY_UNAVAILABLE", "登录安全服务暂时不可用", HttpStatus.SERVICE_UNAVAILABLE); }
            throw new BusinessException("AUTH_INVALID_CREDENTIALS", "用户名或密码错误", HttpStatus.UNAUTHORIZED);
        }
        StpUtil.login(user.getId(), new SaLoginModel().setDevice("admin"));
        users.touchLogin(user.getId());
        try { redis.delete(failureKey); } catch (RuntimeException ex) {
            throw new BusinessException("VALKEY_UNAVAILABLE", "登录安全服务暂时不可用", HttpStatus.SERVICE_UNAVAILABLE);
        }
        return tokenResult(StpUtil.getTokenValue(), user);
    }

    public LoginResult localClientLogin(Long requestedUserId) {
        if (properties.auth() == null || !properties.auth().devClientLoginEnabled())
            throw new BusinessException("AUTH_DEV_LOGIN_DISABLED", "开发登录未启用", HttpStatus.NOT_FOUND);
        UserAccountEntity user = requestedUserId == null ? users.findByUsername("local_client") : users.selectById(requestedUserId);
        if (user == null || !"CLIENT".equals(user.getAccountType()) || !"ENABLED".equals(user.getStatus()))
            throw new BusinessException("AUTH_INVALID_CLIENT", "测试客户不存在", HttpStatus.NOT_FOUND);
        CLIENT_LOGIC.login(user.getId(), new SaLoginModel().setDevice("client-dev"));
        return tokenResult(CLIENT_LOGIC.getTokenValue(), user);
    }

    public CurrentUser adminMe() {
        long id = StpUtil.getLoginIdAsLong();
        UserAccountEntity user = users.selectById(id);
        return currentUser(user);
    }

    public CurrentUser currentUser() {
        if (StpUtil.isLogin()) return adminMe();
        CLIENT_LOGIC.checkLogin();
        long id = CLIENT_LOGIC.getLoginIdAsLong();
        UserAccountEntity user = users.selectById(id);
        return currentUser(user);
    }

    public void logout() { if (StpUtil.isLogin()) StpUtil.logout(); if (CLIENT_LOGIC.isLogin()) CLIENT_LOGIC.logout(); }

    private LoginResult tokenResult(String token, UserAccountEntity user) {
        return new LoginResult(token, "Bearer", 86400,
                new UserBrief(user.getId(), user.getDisplayName(), mask(user.getPhoneMask())));
    }

    private CurrentUser currentUser(UserAccountEntity user) {
        return new CurrentUser(user.getId(), user.getDisplayName(), user.getAccountType(),
                users.findRoles(user.getId()), users.findPermissions(user.getId()), mask(user.getPhoneMask()));
    }

    private String mask(String value) { return value == null ? "" : value; }

    public record LoginResult(String token, String tokenType, long expiresIn, UserBrief user) {}
    public record UserBrief(Long id, String displayName, String phoneMask) {}
    public record CurrentUser(Long id, String displayName, String accountType, List<String> roles,
                              List<String> permissions, String phoneMask) {}
}
