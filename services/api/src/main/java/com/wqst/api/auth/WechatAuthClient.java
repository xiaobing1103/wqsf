package com.wqst.api.auth;

import com.wqst.api.common.BusinessException;
import com.wqst.api.config.AppProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class WechatAuthClient {
    private final AppProperties props;
    public WechatAuthClient(AppProperties props) { this.props = props; }
    public AuthService.LoginResult login(String code) {
        if (props.wechat() == null || props.wechat().appId() == null || props.wechat().appId().isBlank() ||
                props.wechat().appSecret() == null || props.wechat().appSecret().isBlank())
            throw new BusinessException("WECHAT_AUTH_NOT_CONFIGURED", "微信登录尚未配置", HttpStatus.SERVICE_UNAVAILABLE);
        throw new BusinessException("WECHAT_AUTH_NOT_IMPLEMENTED", "微信登录适配器等待正式凭据联调", HttpStatus.NOT_IMPLEMENTED);
    }
}
