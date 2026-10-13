package com.wqst.api.categorymatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.wqst.api.categorymatch.entity.BusinessCategoryRuleEntity;
import com.wqst.api.categorymatch.mapper.BusinessCategoryRuleMapper;
import com.wqst.api.file.FileInspector;
import java.util.List;
import org.junit.jupiter.api.Test;

class CategoryMatchServiceTest {
    private final BusinessCategoryRuleMapper mapper = mock(BusinessCategoryRuleMapper.class);
    private final CategoryMatchService service = new CategoryMatchService(mapper,
            mock(BusinessLicenseOcrClient.class), new FileInspector());

    @Test
    void recommendsAndGroupsCategoriesByMatchedBusinessScopeKeywords() {
        when(mapper.selectList(any())).thenReturn(List.of(
                rule("DIGITAL", "数字与技术服务", "SOFTWARE", "软件与信息技术服务", "软件开发,技术服务,计算机系统", 10),
                rule("DIGITAL", "数字与技术服务", "DATA", "数据与云服务", "大数据,云计算", 20),
                rule("OFFICE", "家居与办公用品", "FURNITURE", "家具与木制品", "家具,木制品", 30)));

        CategoryMatchService.CategoryMatchView result = service.recommend("示例科技公司",
                "一般项目：软件开发、技术服务、云计算装备技术服务");

        assertThat(result.industryCount()).isEqualTo(1);
        assertThat(result.categoryCount()).isEqualTo(2);
        assertThat(result.industries().getFirst().categories())
                .extracting(CategoryMatchService.CategoryRecommendationView::categoryCode)
                .containsExactly("SOFTWARE", "DATA");
        assertThat(result.industries().getFirst().categories().getFirst().matchedKeywords())
                .containsExactly("软件开发", "技术服务");
    }

    @Test
    void returnsEmptyRecommendationInsteadOfInventingCategory() {
        when(mapper.selectList(any())).thenReturn(List.of(
                rule("DIGITAL", "数字与技术服务", "SOFTWARE", "软件与信息技术服务", "软件开发", 10)));

        CategoryMatchService.CategoryMatchView result = service.recommend("示例企业", "企业管理咨询");

        assertThat(result.industryCount()).isZero();
        assertThat(result.categoryCount()).isZero();
        assertThat(result.industries()).isEmpty();
    }

    private BusinessCategoryRuleEntity rule(String industryCode, String industryName, String categoryCode,
                                            String categoryName, String keywords, int sortOrder) {
        BusinessCategoryRuleEntity rule = new BusinessCategoryRuleEntity();
        rule.setIndustryCode(industryCode);
        rule.setIndustryName(industryName);
        rule.setCategoryCode(categoryCode);
        rule.setCategoryName(categoryName);
        rule.setKeywords(keywords);
        rule.setSortOrder(sortOrder);
        rule.setStatus("ENABLED");
        return rule;
    }
}
