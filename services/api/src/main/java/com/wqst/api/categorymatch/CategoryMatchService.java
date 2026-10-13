package com.wqst.api.categorymatch;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wqst.api.categorymatch.entity.BusinessCategoryRuleEntity;
import com.wqst.api.categorymatch.mapper.BusinessCategoryRuleMapper;
import com.wqst.api.file.FileInspector;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CategoryMatchService {
    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;
    private final BusinessCategoryRuleMapper rules;
    private final BusinessLicenseOcrClient ocr;
    private final FileInspector inspector;

    public CategoryMatchService(BusinessCategoryRuleMapper rules, BusinessLicenseOcrClient ocr, FileInspector inspector) {
        this.rules = rules;
        this.ocr = ocr;
        this.inspector = inspector;
    }

    public BusinessLicenseView recognize(MultipartFile file) throws IOException {
        byte[] image = file.getBytes();
        inspector.validate(file.getOriginalFilename(), file.getContentType(), image.length,
                image.length > 16 ? java.util.Arrays.copyOf(image, 16) : image,
                Set.of("jpg", "jpeg", "png"), MAX_IMAGE_BYTES);
        BusinessLicenseOcrClient.OcrResult result = ocr.recognize(image);
        return new BusinessLicenseView(true, result.companyName(), result.businessScope(), result.establishedDate(),
                result.creditCode(), result.registeredCapital(), result.providerRequestId());
    }

    public CategoryMatchView recommend(String companyName, String businessScope) {
        String normalizedScope = businessScope.trim().toLowerCase(Locale.ROOT);
        List<BusinessCategoryRuleEntity> enabledRules = rules.selectList(new LambdaQueryWrapper<BusinessCategoryRuleEntity>()
                .eq(BusinessCategoryRuleEntity::getStatus, "ENABLED")
                .orderByAsc(BusinessCategoryRuleEntity::getSortOrder));

        List<MatchedRule> matched = enabledRules.stream()
                .map(rule -> new MatchedRule(rule, matchedKeywords(rule, normalizedScope)))
                .filter(item -> !item.keywords().isEmpty())
                .sorted(Comparator.<MatchedRule>comparingInt(item -> item.keywords().size()).reversed()
                        .thenComparingInt(item -> safeOrder(item.rule())))
                .toList();

        Map<String, IndustryBuilder> grouped = new LinkedHashMap<>();
        for (MatchedRule item : matched) {
            BusinessCategoryRuleEntity rule = item.rule();
            int score = Math.min(98, 62 + item.keywords().size() * 12);
            CategoryRecommendationView recommendation = new CategoryRecommendationView(rule.getCategoryCode(),
                    rule.getCategoryName(), item.keywords(), score,
                    "经营范围命中“" + String.join("、", item.keywords()) + "”");
            grouped.computeIfAbsent(rule.getIndustryCode(), ignored -> new IndustryBuilder(rule.getIndustryCode(), rule.getIndustryName()))
                    .categories.add(recommendation);
        }

        List<IndustryMatchView> industries = grouped.values().stream()
                .map(group -> new IndustryMatchView(group.industryCode, group.industryName, List.copyOf(group.categories)))
                .toList();
        int categoryCount = industries.stream().mapToInt(group -> group.categories().size()).sum();
        return new CategoryMatchView(companyName == null ? "" : companyName.trim(), businessScope.trim(),
                industries.size(), categoryCount, industries);
    }

    private List<String> matchedKeywords(BusinessCategoryRuleEntity rule, String scope) {
        if (rule.getKeywords() == null || rule.getKeywords().isBlank()) return List.of();
        return java.util.Arrays.stream(rule.getKeywords().split(","))
                .map(String::trim)
                .filter(keyword -> !keyword.isBlank() && scope.contains(keyword.toLowerCase(Locale.ROOT)))
                .distinct()
                .toList();
    }

    private int safeOrder(BusinessCategoryRuleEntity rule) {
        return rule.getSortOrder() == null ? Integer.MAX_VALUE : rule.getSortOrder();
    }

    private record MatchedRule(BusinessCategoryRuleEntity rule, List<String> keywords) {}

    private static final class IndustryBuilder {
        private final String industryCode;
        private final String industryName;
        private final List<CategoryRecommendationView> categories = new ArrayList<>();

        private IndustryBuilder(String industryCode, String industryName) {
            this.industryCode = industryCode;
            this.industryName = industryName;
        }
    }

    public record BusinessLicenseView(boolean recognized, String companyName, String businessScope,
                                      String establishedDate, String creditCode, String registeredCapital,
                                      String providerRequestId) {}
    public record CategoryMatchView(String companyName, String businessScope, int industryCount,
                                    int categoryCount, List<IndustryMatchView> industries) {}
    public record IndustryMatchView(String industryCode, String industryName,
                                    List<CategoryRecommendationView> categories) {}
    public record CategoryRecommendationView(String categoryCode, String categoryName,
                                             List<String> matchedKeywords, int score, String reason) {}
}
