package com.wqst.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("wqst")
public record AppProperties(Security security, Bootstrap bootstrap, Wechat wechat, S3 s3, Export export, Auth auth) {
    public record Security(String encryptionKey) {}
    public record Bootstrap(String adminUsername, String adminPassword) {}
    public record Wechat(String appId, String appSecret) {}
    public record S3(String endpoint, String accessKey, String secretKey, String bucket, String region) {}
    public record Export(long linkTtlMinutes, int workerThreads) {}
    public record Auth(boolean devClientLoginEnabled) {}
}
