package com.wqst.api.auth;

import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import com.wqst.api.account.entity.UserAccountEntity;
import com.wqst.api.account.mapper.UserAccountMapper;
import com.wqst.api.common.BusinessException;
import com.wqst.api.config.AppProperties;
import java.time.Duration;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuthService {
    public static final StpLogic CLIENT_LOGIC = new StpLogic("client");
    private final UserAccountMapper users;
    private final PasswordEncoder passwords;
    private final AppProperties properties;
    private final StringRedisTemplate redis;
    private final WechatAuthClient wechat;

    public AuthService(UserAccountMapper users, PasswordEncoder passwords, AppProperties properties,
                       StringRedisTemplate redis, WechatAuthClient wechat) {
        this.users = users; this.passwords = passwords; this.properties = properties; this.redis = redis; this.wechat = wechat;
    }

    public LoginResult adminLogin(String username, String password) {
        String normalized = username.trim();
        String failureKey = failureKey("admin", normalized);
        checkRateLimit(failureKey);
        UserAccountEntity user = users.findByUsername(normalized);
        if (user == null || !"ADMIN".equals(user.getAccountType()) || !"ENABLED".equals(user.getStatus()) ||
                !passwordMatches(password, user.getPasswordHash())) {
            recordFailure(failureKey);
            throw new BusinessException("AUTH_INVALID_CREDENTIALS", "用户名或密码错误", HttpStatus.UNAUTHORIZED);
        }
        StpUtil.login(user.getId(), new SaLoginModel().setDevice("admin"));
        users.touchLogin(user.getId());
        clearFailures(failureKey);
        return tokenResult(StpUtil.getTokenValue(), user);
    }

    public LoginResult clientLogin(String username, String password, boolean bindWechat, String wechatCode) {
        String normalized = username.trim();
        String failureKey = failureKey("client", normalized);
        checkRateLimit(failureKey);
        UserAccountEntity user = users.findByUsername(normalized);
        if (user == null || !"CLIENT".equals(user.getAccountType()) || !"ENABLED".equals(user.getStatus()) ||
                !passwordMatches(password, user.getPasswordHash())) {
            recordFailure(failureKey);
            throw new BusinessException("AUTH_INVALID_CREDENTIALS", "用户名或密码错误", HttpStatus.UNAUTHORIZED);
        }
        if (bindWechat) {
            if (!StringUtils.hasText(wechatCode))
                throw new BusinessException("WECHAT_CODE_REQUIRED", "绑定微信需要重新获取微信登录凭证", HttpStatus.BAD_REQUEST);
            bindWechat(user, wechat.exchange(wechatCode.trim()));
        }
        CLIENT_LOGIC.login(user.getId(), new SaLoginModel().setDevice("client-password"));
        users.touchLogin(user.getId());
        clearFailures(failureKey);
        return tokenResult(CLIENT_LOGIC.getTokenValue(), user);
    }

    public LoginResult wechatLogin(WechatAuthClient.WechatIdentity identity) {
        UserAccountEntity user = users.findByWechatOpenid(identity.openId());
        if (user == null)
            throw new BusinessException("WECHAT_ACCOUNT_UNBOUND", "当前微信尚未绑定客户账号，请先使用账号密码登录并绑定微信", HttpStatus.CONFLICT);
        if (!"CLIENT".equals(user.getAccountType()) || !"ENABLED".equals(user.getStatus()))
            throw new BusinessException("AUTH_INVALID_CLIENT", "客户账号不可用，请联系管理员", HttpStatus.FORBIDDEN);
        CLIENT_LOGIC.login(user.getId(), new SaLoginModel().setDevice("client-wechat"));
        users.touchLogin(user.getId());
        return tokenResult(CLIENT_LOGIC.getTokenValue(), user);
    }

    public LoginResult localClientLogin(Long requestedUserId) {
        if (properties.auth() == null || !properties.auth().devClientLoginEnabled())
            throw new BusinessException("AUTH_DEV_LOGIN_DISABLED", "开发登录未启用", HttpStatus.NOT_FOUND);
        UserAccountEntity user = requestedUserId == null ? users.findByUsername("local_client") : users.selectById(requestedUserId);
        if (user == null || !"CLIENT".equals(user.getAccountType()) || !"ENABLED".equals(user.getStatus()))
            throw new BusinessException("AUTH_INVALID_CLIENT", "测试客户不存在", HttpStatus.NOT_FOUND);
        CLIENT_LOGIC.login(user.getId(), new SaLoginModel().setDevice("client-dev"));
        users.touchLogin(user.getId());
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

    private void bindWechat(UserAccountEntity user, WechatAuthClient.WechatIdentity identity) {
        UserAccountEntity bound = users.findByWechatOpenid(identity.openId());
        if (bound != null && !bound.getId().equals(user.getId()))
            throw new BusinessException("WECHAT_ALREADY_BOUND", "当前微信已绑定其他客户账号，请直接使用微信登录或联系管理员处理", HttpStatus.CONFLICT);
        if (StringUtils.hasText(user.getWechatOpenid()) && !user.getWechatOpenid().equals(identity.openId()))
            throw new BusinessException("ACCOUNT_WECHAT_ALREADY_BOUND", "该客户账号已绑定其他微信，不能重复绑定", HttpStatus.CONFLICT);
        if (identity.openId().equals(user.getWechatOpenid())) return;
        user.setWechatOpenid(identity.openId());
        if (!StringUtils.hasText(user.getWechatUnionid()) && StringUtils.hasText(identity.unionId()))
            user.setWechatUnionid(identity.unionId());
        try {
            if (users.updateById(user) == 0)
                throw new BusinessException("ACCOUNT_UPDATE_CONFLICT", "账号信息已更新，请重新登录", HttpStatus.CONFLICT);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException("WECHAT_ALREADY_BOUND", "当前微信已绑定其他客户账号", HttpStatus.CONFLICT);
        }
    }

    private boolean passwordMatches(String password, String hash) {
        return StringUtils.hasText(hash) && passwords.matches(password, hash);
    }

    private String failureKey(String scope, String username) {
        return "wqst:auth:fail:" + scope + ":" + username.toLowerCase();
    }

    private void checkRateLimit(String key) {
        try {
            String attempts = redis.opsForValue().get(key);
            if (attempts != null && Integer.parseInt(attempts) >= 5)
                throw new BusinessException("AUTH_RATE_LIMITED", "登录失败次数过多，请 15 分钟后再试", HttpStatus.TOO_MANY_REQUESTS);
        } catch (BusinessException ex) { throw ex; } catch (RuntimeException ex) {
            throw new BusinessException("VALKEY_UNAVAILABLE", "登录安全服务暂时不可用", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private void recordFailure(String key) {
        try {
            Long value = redis.opsForValue().increment(key);
            if (value != null && value == 1) redis.expire(key, Duration.ofMinutes(15));
        } catch (RuntimeException ex) {
            throw new BusinessException("VALKEY_UNAVAILABLE", "登录安全服务暂时不可用", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private void clearFailures(String key) {
        try { redis.delete(key); } catch (RuntimeException ex) {
            throw new BusinessException("VALKEY_UNAVAILABLE", "登录安全服务暂时不可用", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

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
