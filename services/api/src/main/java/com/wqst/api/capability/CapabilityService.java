package com.wqst.api.capability;

import com.wqst.api.audit.AuditService;
import com.wqst.api.common.BusinessException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CapabilityService {
    public static final List<String> KEYS = List.of("tradeApplicationEnabled", "trademarkConsultationEnabled",
            "qualificationConsultationEnabled", "categoryMatchEnabled", "licenseOcrEnabled",
            "sensitiveMaterialUploadEnabled", "customerServiceEnabled");
    private final JdbcTemplate jdbc;
    private final AuditService audit;
    public CapabilityService(JdbcTemplate jdbc, AuditService audit) {
        this.jdbc = jdbc;
        this.audit = audit;
    }

    public Map<String, Boolean> capabilities() {
        Map<String, Boolean> result = new LinkedHashMap<>();
        KEYS.forEach(key -> result.put(key, false));
        for (Setting setting : settings()) result.put(setting.key(), setting.enabled());
        if (!result.get("categoryMatchEnabled")) result.put("licenseOcrEnabled", false);
        return result;
    }

    public List<Setting> settings() {
        return jdbc.query("SELECT capability_key, enabled, version FROM client_capability ORDER BY capability_key",
                // 保留旧响应字段兼容已生成的客户端；七项均由超级管理员直接管理。
                (rs, row) -> new Setting(rs.getString(1), rs.getBoolean(2), rs.getInt(3), true))
                .stream().filter(s -> KEYS.contains(s.key())).toList();
    }

    public boolean enabled(String key) { return Boolean.TRUE.equals(capabilities().get(key)); }

    public void require(String key) {
        if (!enabled(key)) throw new BusinessException("FEATURE_DISABLED", "该功能暂未开放，请联系客服了解服务范围", HttpStatus.FORBIDDEN);
    }

    public String serviceKey(String serviceType) {
        return switch (serviceType == null ? "" : serviceType) {
            case "TRADE_INCREMENT" -> "tradeApplicationEnabled";
            case "IP" -> "trademarkConsultationEnabled";
            case "QUALIFICATION" -> "qualificationConsultationEnabled";
            default -> "unknown";
        };
    }

    @Transactional
    public List<Setting> update(String key, boolean enabled, int version, String reason, long userId) {
        if (!KEYS.contains(key)) throw new BusinessException("CAPABILITY_NOT_FOUND", "功能配置不存在", HttpStatus.NOT_FOUND);
        int changed = jdbc.update("UPDATE client_capability SET enabled=?,version=version+1,updated_by=?,updated_at=CURRENT_TIMESTAMP WHERE capability_key=? AND version=?",
                enabled, userId, key, version);
        if (changed != 1) throw new BusinessException("OPTIMISTIC_LOCK_CONFLICT", "配置已变化，请刷新后重试", HttpStatus.CONFLICT);
        audit.operation(userId, "CAPABILITY_UPDATE", "CLIENT_CAPABILITY", null, key + "=" + enabled + "；原因：" + reason.trim());
        return settings();
    }

    public record Setting(String key, boolean enabled, int version, boolean releaseAllowed) {}
}
