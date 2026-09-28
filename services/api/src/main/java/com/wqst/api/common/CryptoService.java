package com.wqst.api.common;

import com.wqst.api.config.AppProperties;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class CryptoService {
    private static final int IV_BYTES = 12;
    private final SecretKeySpec aesKey;
    private final SecretKeySpec hmacKey;
    private final SecureRandom random = new SecureRandom();

    public CryptoService(AppProperties properties) {
        try {
            byte[] source = properties.security().encryptionKey().getBytes(StandardCharsets.UTF_8);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(source);
            this.aesKey = new SecretKeySpec(digest, "AES");
            this.hmacKey = new SecretKeySpec(digest, "HmacSHA256");
        } catch (Exception ex) {
            throw new IllegalStateException("无法初始化字段加密组件", ex);
        }
    }

    public String encrypt(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(value.trim().getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array());
        } catch (Exception ex) {
            throw new IllegalStateException("字段加密失败", ex);
        }
    }

    public String decrypt(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            byte[] packed = Base64.getDecoder().decode(value);
            byte[] iv = new byte[IV_BYTES];
            byte[] encrypted = new byte[packed.length - IV_BYTES];
            System.arraycopy(packed, 0, iv, 0, IV_BYTES);
            System.arraycopy(packed, IV_BYTES, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("字段解密失败", ex);
        }
    }

    public String hash(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(hmacKey);
            return java.util.HexFormat.of().formatHex(mac.doFinal(value.trim().getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("字段摘要失败", ex);
        }
    }

    public String maskPhone(String value) {
        if (value == null || value.isBlank()) return null;
        String v = value.replaceAll("\\s+", "");
        if (v.length() < 7) return "****";
        return v.substring(0, 3) + " **** " + v.substring(v.length() - 4);
    }

    public String maskAccount(String value) {
        if (value == null || value.isBlank()) return null;
        String v = value.replaceAll("\\s+", "");
        return "********" + v.substring(Math.max(0, v.length() - 4));
    }

    public String maskTaxNo(String value) {
        if (value == null || value.isBlank()) return null;
        String v = value.trim();
        if (v.length() < 6) return "******";
        return v.substring(0, 4) + "**********" + v.substring(v.length() - 2);
    }
}
