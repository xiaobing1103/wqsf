package com.wqst.api.common;

public final class Enums {
    private Enums() {}
    public enum CaseStatus { DRAFT, PENDING_REVIEW, NEED_SUPPLEMENT, PROCESSING, COMPLETED, CANCELED }
    public enum ReviewStatus { PENDING, PASSED, NEED_SUPPLEMENT, NOT_REQUIRED }
    public enum JobStatus { PENDING, RUNNING, SUCCEEDED, FAILED, EXPIRED, CANCELED }
    public enum ServiceType { TRADE_INCREMENT, IP, QUALIFICATION }
}
