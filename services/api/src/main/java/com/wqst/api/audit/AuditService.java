package com.wqst.api.audit;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wqst.api.audit.entity.DownloadLogEntity;
import com.wqst.api.audit.entity.FileAccessLogEntity;
import com.wqst.api.audit.entity.OperationLogEntity;
import com.wqst.api.audit.mapper.DownloadLogMapper;
import com.wqst.api.audit.mapper.FileAccessLogMapper;
import com.wqst.api.audit.mapper.OperationLogMapper;
import com.wqst.api.common.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final OperationLogMapper operations;
    private final FileAccessLogMapper fileAccess;
    private final DownloadLogMapper downloads;
    private final HttpServletRequest request;
    public AuditService(OperationLogMapper operations, FileAccessLogMapper fileAccess, DownloadLogMapper downloads, HttpServletRequest request) {
        this.operations = operations; this.fileAccess = fileAccess; this.downloads = downloads; this.request = request;
    }
    public void operation(Long operatorId, String code, String type, Long businessId, String summary) {
        OperationLogEntity log = new OperationLogEntity(); log.setOperatorId(operatorId); log.setOperationCode(code);
        log.setBusinessType(type); log.setBusinessId(businessId); log.setSummary(summary); log.setRequestId(MDC.get("requestId"));
        log.setIpAddress(clientIp()); operations.insert(log);
    }
    public void file(Long operatorId, Long fileId, Long caseId, String type, String result) {
        FileAccessLogEntity log = new FileAccessLogEntity(); log.setOperatorId(operatorId); log.setFileId(fileId); log.setCaseId(caseId);
        log.setAccessType(type); log.setResult(result); log.setRequestId(MDC.get("requestId")); log.setIpAddress(clientIp()); fileAccess.insert(log);
    }
    public void download(Long operatorId, Long jobId, String result) {
        DownloadLogEntity log = new DownloadLogEntity(); log.setOperatorId(operatorId); log.setExportJobId(jobId); log.setResult(result);
        log.setIpAddress(clientIp()); log.setUserAgent(limit(request.getHeader("User-Agent"), 500)); downloads.insert(log);
    }
    public PageResponse<OperationLogEntity> operations(int page, int size) {
        Page<OperationLogEntity> result = operations.selectPage(Page.of(page, size), new LambdaQueryWrapper<OperationLogEntity>().orderByDesc(OperationLogEntity::getCreatedAt));
        return PageResponse.of(result.getRecords(), page, size, result.getTotal());
    }
    public PageResponse<FileAccessLogEntity> files(int page, int size) {
        Page<FileAccessLogEntity> result = fileAccess.selectPage(Page.of(page, size), new LambdaQueryWrapper<FileAccessLogEntity>().orderByDesc(FileAccessLogEntity::getCreatedAt));
        return PageResponse.of(result.getRecords(), page, size, result.getTotal());
    }
    public PageResponse<DownloadLogEntity> downloads(int page, int size) {
        Page<DownloadLogEntity> result = downloads.selectPage(Page.of(page, size), new LambdaQueryWrapper<DownloadLogEntity>().orderByDesc(DownloadLogEntity::getCreatedAt));
        return PageResponse.of(result.getRecords(), page, size, result.getTotal());
    }
    private String clientIp() { String forwarded=request.getHeader("X-Forwarded-For"); return limit(forwarded == null ? request.getRemoteAddr() : forwarded.split(",")[0].trim(), 64); }
    private String limit(String value, int max) { return value == null ? null : value.substring(0, Math.min(value.length(), max)); }
}
