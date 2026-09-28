package com.wqst.api.common;

import com.wqst.api.common.Enums.CaseStatus;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class CaseStateMachine {
    private static final Map<CaseStatus, EnumSet<CaseStatus>> TRANSITIONS = new EnumMap<>(CaseStatus.class);
    static {
        TRANSITIONS.put(CaseStatus.DRAFT, EnumSet.of(CaseStatus.PENDING_REVIEW));
        TRANSITIONS.put(CaseStatus.PENDING_REVIEW, EnumSet.of(CaseStatus.NEED_SUPPLEMENT, CaseStatus.PROCESSING, CaseStatus.CANCELED));
        TRANSITIONS.put(CaseStatus.NEED_SUPPLEMENT, EnumSet.of(CaseStatus.PENDING_REVIEW, CaseStatus.CANCELED));
        TRANSITIONS.put(CaseStatus.PROCESSING, EnumSet.of(CaseStatus.COMPLETED, CaseStatus.NEED_SUPPLEMENT, CaseStatus.CANCELED));
        TRANSITIONS.put(CaseStatus.COMPLETED, EnumSet.noneOf(CaseStatus.class));
        TRANSITIONS.put(CaseStatus.CANCELED, EnumSet.noneOf(CaseStatus.class));
    }

    public void check(String current, String target) {
        CaseStatus from;
        CaseStatus to;
        try {
            from = CaseStatus.valueOf(current);
            to = CaseStatus.valueOf(target);
        } catch (RuntimeException ex) {
            throw new BusinessException("CASE_STATUS_INVALID", "报单状态无效", HttpStatus.BAD_REQUEST);
        }
        if (!TRANSITIONS.getOrDefault(from, EnumSet.noneOf(CaseStatus.class)).contains(to)) {
            throw new BusinessException("CASE_STATUS_TRANSITION_DENIED", "不允许从 " + from + " 流转到 " + to, HttpStatus.CONFLICT);
        }
    }
}
