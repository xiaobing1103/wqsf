package com.wqst.api.auth;

import com.wqst.api.common.BusinessException;
import com.wqst.api.config.AppProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class WechatAuthClient {
    private final AppProperties props;
    private final RestClient client;

    @org.springframework.beans.factory.annotation.Autowired
    public WechatAuthClient(AppProperties props) {
        this(props, RestClient.builder().requestFactory(new SimpleClientHttpRequestFactory()));
    }

    WechatAuthClient(AppProperties props, RestClient.Builder builder) {
        this.props = props;
        this.client = builder.baseUrl("https://api.weixin.qq.com").build();
    }

    public WechatIdentity exchange(String code) {
        if (props.wechat() == null || props.wechat().appId() == null || props.wechat().appId().isBlank() ||
                props.wechat().appSecret() == null || props.wechat().appSecret().isBlank())
            throw new BusinessException("WECHAT_AUTH_NOT_CONFIGURED", "微信登录尚未配置", HttpStatus.SERVICE_UNAVAILABLE);
        try {
            WechatSession response = client.get().uri(builder -> builder.path("/sns/jscode2session")
                    .queryParam("appid", props.wechat().appId())
                    .queryParam("secret", props.wechat().appSecret())
                    .queryParam("js_code", code)
                    .queryParam("grant_type", "authorization_code")
                    .build()).retrieve().body(WechatSession.class);
            if (response == null || response.errcode() != null || response.openid() == null || response.openid().isBlank()) {
                throw new BusinessException("WECHAT_CODE_INVALID", "微信登录凭证无效或已过期，请重试", HttpStatus.UNAUTHORIZED);
            }
            return new WechatIdentity(response.openid(), response.unionid());
        } catch (BusinessException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new BusinessException("WECHAT_AUTH_UNAVAILABLE", "微信登录服务暂时不可用，请稍后重试", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    record WechatSession(String openid, String unionid, String sessionKey, Integer errcode, String errmsg) {}
    public record WechatIdentity(String openId, String unionId) {}
}
