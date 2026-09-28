package com.wqst.api.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class IdempotencyService {
    private final StringRedisTemplate redis;
    private final ObjectMapper json;
    private final TransactionTemplate transactions;

    public IdempotencyService(StringRedisTemplate redis, ObjectMapper json, PlatformTransactionManager transactionManager) {
        this.redis = redis;
        this.json = json;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    public <T> T execute(String actorScope, String operation, String key, TypeReference<T> type, Supplier<T> action) {
        if (!StringUtils.hasText(key) || key.length() > 128) {
            throw new BusinessException("IDEMPOTENCY_KEY_REQUIRED", "请提供有效的 Idempotency-Key", HttpStatus.BAD_REQUEST);
        }
        String base = "wqst:idempotency:" + operation + ":" + actorScope + ":" + hashKey(key);
        String resultKey = base + ":result";
        try {
            String cached = redis.opsForValue().get(resultKey);
            if (cached != null) return json.readValue(cached, type);
            Boolean locked = redis.opsForValue().setIfAbsent(base + ":lock", "1", Duration.ofMinutes(2));
            if (!Boolean.TRUE.equals(locked)) {
                throw new BusinessException("IDEMPOTENCY_IN_PROGRESS", "相同请求正在处理中", HttpStatus.CONFLICT);
            }
            try {
                AtomicReference<String> encoded = new AtomicReference<>();
                T result = transactions.execute(status -> {
                    T value = action.get();
                    try { encoded.set(json.writeValueAsString(value)); }
                    catch (Exception ex) { throw new IllegalStateException("幂等结果序列化失败", ex); }
                    return value;
                });
                redis.opsForValue().set(resultKey, encoded.get(), Duration.ofHours(24));
                return result;
            } finally {
                redis.delete(base + ":lock");
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (DataAccessException ex) {
            throw new BusinessException("VALKEY_UNAVAILABLE", "幂等服务暂时不可用", HttpStatus.SERVICE_UNAVAILABLE);
        } catch (Exception ex) {
            throw new IllegalStateException("幂等结果处理失败", ex);
        }
    }

    private String hashKey(String key) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("无法计算幂等键摘要", ex);
        }
    }
}
