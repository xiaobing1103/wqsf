package com.wqst.api.dashboard;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wqst.api.caseorder.entity.ServiceCaseEntity;
import com.wqst.api.caseorder.mapper.ServiceCaseMapper;
import com.wqst.api.company.entity.CompanyEntity;
import com.wqst.api.company.mapper.CompanyMapper;
import com.wqst.api.exports.ExportJobEntity;
import com.wqst.api.exports.ExportJobMapper;
import com.wqst.api.imports.ImportJobEntity;
import com.wqst.api.imports.ImportJobMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
@Service public class DashboardService {
    private final ServiceCaseMapper cases;private final CompanyMapper companies;private final ImportJobMapper imports;private final ExportJobMapper exports;
    public DashboardService(ServiceCaseMapper cases,CompanyMapper companies,ImportJobMapper imports,ExportJobMapper exports){this.cases=cases;this.companies=companies;this.imports=imports;this.exports=exports;}
    public DashboardView view(){Map<String,Long> statuses=new LinkedHashMap<>();for(String s:List.of("DRAFT","PENDING_REVIEW","NEED_SUPPLEMENT","PROCESSING","COMPLETED","CANCELED"))statuses.put(s,cases.selectCount(new LambdaQueryWrapper<ServiceCaseEntity>().eq(ServiceCaseEntity::getStatus,s).isNull(ServiceCaseEntity::getDeletedAt)));long companyCount=companies.selectCount(new LambdaQueryWrapper<CompanyEntity>().eq(CompanyEntity::getStatus,"ENABLED").isNull(CompanyEntity::getDeletedAt));long pendingImports=imports.selectCount(new LambdaQueryWrapper<ImportJobEntity>().in(ImportJobEntity::getStatus,List.of("PENDING","RUNNING")));long pendingExports=exports.selectCount(new LambdaQueryWrapper<ExportJobEntity>().in(ExportJobEntity::getStatus,List.of("PENDING","RUNNING")));return new DashboardView(companyCount,statuses,pendingImports,pendingExports);}
    public record DashboardView(long enabledCompanies,Map<String,Long> caseStatuses,long pendingImports,long pendingExports){}
}
