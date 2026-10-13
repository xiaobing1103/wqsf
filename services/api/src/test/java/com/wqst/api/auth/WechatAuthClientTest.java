package com.wqst.api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.wqst.api.common.BusinessException;
import com.wqst.api.config.AppProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class WechatAuthClientTest {
    @Test
    void exchangesTemporaryCodeWithoutExposingSessionKey() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        WechatAuthClient client = new WechatAuthClient(properties("test-app", "test-secret"), builder);
        server.expect(requestTo("https://api.weixin.qq.com/sns/jscode2session?appid=test-app&secret=test-secret&js_code=temp-code&grant_type=authorization_code"))
                .andRespond(withSuccess("{\"openid\":\"openid-001\",\"unionid\":\"union-001\",\"session_key\":\"do-not-return\"}", MediaType.APPLICATION_JSON));

        WechatAuthClient.WechatIdentity identity = client.exchange("temp-code");

        assertThat(identity.openId()).isEqualTo("openid-001");
        assertThat(identity.unionId()).isEqualTo("union-001");
        server.verify();
    }

    @Test
    void rejectsExpiredWechatCode() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        WechatAuthClient client = new WechatAuthClient(properties("test-app", "test-secret"), builder);
        server.expect(requestTo("https://api.weixin.qq.com/sns/jscode2session?appid=test-app&secret=test-secret&js_code=expired&grant_type=authorization_code"))
                .andRespond(withSuccess("{\"errcode\":40029,\"errmsg\":\"invalid code\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.exchange("expired"))
                .isInstanceOfSatisfying(BusinessException.class, ex -> assertThat(ex.code()).isEqualTo("WECHAT_CODE_INVALID"));
        server.verify();
    }

    @Test
    void refusesWechatLoginWithoutServerCredentials() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer.bindTo(builder).build();
        WechatAuthClient client = new WechatAuthClient(properties("", ""), builder);
        assertThatThrownBy(() -> client.exchange("code"))
                .isInstanceOfSatisfying(BusinessException.class, ex -> assertThat(ex.code()).isEqualTo("WECHAT_AUTH_NOT_CONFIGURED"));
    }

    private AppProperties properties(String appId, String appSecret) {
        return new AppProperties(new AppProperties.Security("test-key"), new AppProperties.Bootstrap("", ""),
                new AppProperties.Wechat(appId, appSecret), new AppProperties.S3("http://localhost", "a", "b", "c", "r"),
                new AppProperties.Export(30, 1), new AppProperties.Auth(true),
                new AppProperties.Ocr("", "", "ap-guangzhou", "ocr.tencentcloudapi.com"));
    }
}
