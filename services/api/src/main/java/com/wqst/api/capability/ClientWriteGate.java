package com.wqst.api.capability;

import com.wqst.api.caseorder.CaseService;
import com.wqst.api.common.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** 只管客户新增/修改，已提交报单及附件的授权查询不受开关影响。 */
@Component
public class ClientWriteGate {
    private final CapabilityService capabilities;
    private final CaseService cases;
    public ClientWriteGate(CapabilityService capabilities, CaseService cases) { this.capabilities=capabilities; this.cases=cases; }

    public void create(String serviceType) { capabilities.require(capabilities.serviceKey(serviceType)); }

    public void write(long caseId, long userId, Long materialId, boolean sensitiveOperation) {
        CaseService.CaseView detail=cases.clientDetail(caseId, userId); // 先校验企业范围，再读取开关。
        create(detail.serviceType());
        boolean sensitive=sensitiveOperation;
        if (materialId != null) {
            CaseService.MaterialView material=detail.materials().stream().filter(m -> materialId.equals(m.id())).findFirst()
                    .orElseThrow(() -> new BusinessException("MATERIAL_INPUT_INVALID", "资料不属于当前报单", HttpStatus.BAD_REQUEST));
            sensitive |= material.sensitive();
        }
        if (sensitive) capabilities.require("sensitiveMaterialUploadEnabled");
    }
}
