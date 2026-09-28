package com.wqst.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;

@Configuration
public class OpenApiConfig {
    public static final String API_TITLE = "万企商服通接口文档";
    public static final String API_VERSION = "1.0.0";
    public static final String SECURITY_SCHEME = "BearerAuth";

    private static final Map<String, String> CONTROLLER_TAGS = Map.ofEntries(
            Map.entry("AdminAccountController", "后台账号"),
            Map.entry("AuditController", "审计日志"),
            Map.entry("AuthController", "认证与会话"),
            Map.entry("AdminCaseController", "后台报单"),
            Map.entry("CaseController", "客户报单"),
            Map.entry("AdminProductController", "后台产品"),
            Map.entry("CatalogController", "服务目录"),
            Map.entry("AdminCompanyController", "后台企业"),
            Map.entry("CompanyController", "客户企业"),
            Map.entry("DashboardController", "后台工作台"),
            Map.entry("ExportController", "后台导出"),
            Map.entry("FileController", "文件管理"),
            Map.entry("ImportController", "后台导入"),
            Map.entry("NotificationController", "客户通知"));

    private static final Map<String, String> SUMMARIES = summaries();
    private static final Set<String> PUBLIC_OPERATIONS = Set.of(
            "authAdminLogin", "authDevLogin", "authWechat", "catalogCatalog", "catalogTemplate");

    private static final Map<String, String> FIELD_DESCRIPTIONS = Map.ofEntries(
            Map.entry("id", "主键编号"), Map.entry("userId", "用户编号"), Map.entry("companyId", "企业编号"),
            Map.entry("caseId", "报单编号"), Map.entry("productId", "产品编号"), Map.entry("materialId", "资料项编号"),
            Map.entry("jobId", "任务编号"), Map.entry("templateId", "模板编号"),
            Map.entry("sourceTemplateId", "来源模板编号"), Map.entry("username", "登录用户名"),
            Map.entry("password", "登录密码"),
            Map.entry("token", "访问令牌"), Map.entry("tokenType", "令牌类型"),
            Map.entry("expiresIn", "令牌有效秒数"), Map.entry("expiresAt", "失效时间"),
            Map.entry("displayName", "用户显示名称"), Map.entry("accountType", "账号类型"),
            Map.entry("roles", "角色编码列表"), Map.entry("permissions", "权限编码列表"),
            Map.entry("phone", "联系电话"), Map.entry("phoneMask", "脱敏手机号"),
            Map.entry("companyName", "公司名称"), Map.entry("creditCode", "统一社会信用代码"),
            Map.entry("creditCodeMask", "脱敏统一社会信用代码"), Map.entry("contactName", "联系人姓名"),
            Map.entry("contactPhone", "联系电话"), Map.entry("contactPhoneMask", "脱敏联系电话"),
            Map.entry("legalPerson", "法定代表人"), Map.entry("legalPersonName", "法人姓名"),
            Map.entry("province", "省份"), Map.entry("city", "城市"), Map.entry("companyEmail", "公司邮箱"),
            Map.entry("detailAddress", "详细地址"), Map.entry("contactAddress", "联系地址"),
            Map.entry("taxNo", "纳税人识别号"), Map.entry("taxNoMask", "脱敏纳税人识别号"),
            Map.entry("bankBranch", "开户行网点"), Map.entry("basicAccount", "基本户账号"),
            Map.entry("basicAccountMask", "脱敏基本户账号"), Map.entry("memberRole", "企业成员角色"),
            Map.entry("serviceType", "服务类型"), Map.entry("caseMonth", "报单月份"),
            Map.entry("caseNo", "报单业务编号"), Map.entry("status", "业务状态"),
            Map.entry("targetStatus", "目标状态"), Map.entry("note", "状态说明"),
            Map.entry("materials", "资料项及完成度"), Map.entry("materialIds", "资料项编号列表"),
            Map.entry("materialCode", "资料项编码"), Map.entry("materialName", "资料项名称"),
            Map.entry("inputType", "录入类型"), Map.entry("required", "是否必填"),
            Map.entry("sensitive", "是否敏感"), Map.entry("textValue", "文字或链接内容"),
            Map.entry("submitStatus", "提交状态"), Map.entry("reviewStatus", "审核状态"),
            Map.entry("clientVisibleNote", "客户可见说明"), Map.entry("internalNote", "内部备注"),
            Map.entry("clientMessage", "客户补件说明"), Map.entry("value", "文字输入内容"),
            Map.entry("consent", "是否同意提交"), Map.entry("version", "乐观锁版本号"),
            Map.entry("page", "页码，从 1 开始"), Map.entry("pageSize", "每页数量"),
            Map.entry("keyword", "搜索关键词"), Map.entry("assigneeId", "经办人编号"),
            Map.entry("file", "待上传文件"), Map.entry("fileId", "文件编号"),
            Map.entry("originalName", "原文件名"), Map.entry("contentType", "文件媒体类型"),
            Map.entry("extension", "文件扩展名"), Map.entry("fileSize", "文件大小，单位字节"),
            Map.entry("sha256", "文件摘要"), Map.entry("previewUrl", "短时有效预览地址"),
            Map.entry("downloadUrl", "短时有效下载地址"), Map.entry("dryRun", "是否仅校验不写入"),
            Map.entry("includeSensitive", "是否包含敏感资料"), Map.entry("caseIds", "报单编号列表"),
            Map.entry("success", "请求是否成功"), Map.entry("code", "业务结果编码"), Map.entry("message", "中文结果说明"),
            Map.entry("data", "业务数据"), Map.entry("requestId", "请求追踪编号"),
            Map.entry("timestamp", "响应时间"), Map.entry("items", "当前页数据"),
            Map.entry("total", "总记录数"), Map.entry("totalPages", "总页数"),
            Map.entry("createdAt", "创建时间"), Map.entry("updatedAt", "更新时间"),
            Map.entry("completedAt", "完成时间"), Map.entry("submittedAt", "提交时间"));

    @Bean
    OpenAPI wanqiOpenApi() {
        return new OpenAPI()
                .info(new Info().title(API_TITLE).version(API_VERSION)
                        .description("面向万企商服通小程序和管理后台的统一接口契约。除明确标记为公开的接口外，均需携带 Bearer Token。"))
                .servers(List.of(new Server().url("/").description("当前服务地址")))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME,
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer")
                                .bearerFormat("Sa-Token").description("登录接口返回的访问令牌")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME))
                .tags(CONTROLLER_TAGS.values().stream().distinct().map(name -> new Tag().name(name)).toList());
    }

    @Bean
    OperationCustomizer chineseOperationCustomizer() {
        return (operation, handlerMethod) -> customize(operation, handlerMethod.getBeanType(), handlerMethod.getMethod());
    }

    Operation customize(Operation operation, Class<?> beanType, Method method) {
        String controller = beanType.getSimpleName();
        String key = controller + "#" + method.getName();
        String operationId = operationId(controller, method.getName());
        String summary = SUMMARIES.get(key);
        if (summary == null) throw new IllegalStateException("缺少 OpenAPI 中文说明: " + key);

        operation.setOperationId(operationId);
        operation.setSummary(summary);
        operation.setDescription(summary + "。响应统一使用 ApiResponse 结构。 ");
        operation.setTags(List.of(CONTROLLER_TAGS.get(controller)));
        if (PUBLIC_OPERATIONS.contains(operationId)) operation.setSecurity(List.of());
        if (operation.getParameters() != null) operation.getParameters().forEach(parameter ->
                parameter.setDescription(FIELD_DESCRIPTIONS.getOrDefault(parameter.getName(), "接口参数")));
        if (operation.getRequestBody() != null) operation.getRequestBody().setDescription("请求参数");
        if (operation.getResponses() != null) {
            operation.getResponses().forEach((status, response) -> response.setDescription(responseDescription(status)));
        }
        return operation;
    }

    @Bean
    OpenApiCustomizer chineseSchemaCustomizer() {
        return openApi -> {
            if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) return;
            openApi.getComponents().getSchemas().forEach(this::describeSchema);
        };
    }

    private void describeSchema(String schemaName, Schema<?> schema) {
        if (schema.getProperties() == null) return;
        schema.getProperties().forEach((name, property) -> {
            Schema<?> field = (Schema<?>) property;
            if (field.getDescription() == null || field.getDescription().isBlank()) {
                String description = FIELD_DESCRIPTIONS.getOrDefault(name, "接口字段");
                if ("WechatLoginRequest".equals(schemaName) && "code".equals(name)) description = "微信临时登录凭证";
                field.setDescription(description);
            }
        });
    }

    static String operationId(String controller, String method) {
        String prefix = controller.endsWith("Controller")
                ? controller.substring(0, controller.length() - "Controller".length()) : controller;
        return Character.toLowerCase(prefix.charAt(0)) + prefix.substring(1)
                + Character.toUpperCase(method.charAt(0)) + method.substring(1);
    }

    private String responseDescription(String status) {
        return switch (status) {
            case "200" -> "操作成功";
            case "201" -> "创建成功";
            case "400" -> "请求参数不正确";
            case "401" -> "未登录或令牌已失效";
            case "403" -> "没有操作权限";
            case "404" -> "业务数据不存在";
            case "409" -> "业务状态冲突";
            case "429" -> "请求过于频繁";
            case "500" -> "服务内部错误";
            case "503" -> "依赖服务暂时不可用";
            default -> "接口响应";
        };
    }

    private static Map<String, String> summaries() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("AdminAccountController#list", "查询后台账号列表");
        values.put("AdminAccountController#create", "创建后台或客户账号");
        values.put("AdminAccountController#roles", "修改账号角色");
        values.put("AuditController#operations", "查询操作日志");
        values.put("AuditController#files", "查询文件访问日志");
        values.put("AuditController#downloads", "查询下载日志");
        values.put("AuthController#adminLogin", "后台账号登录");
        values.put("AuthController#devLogin", "本地开发客户登录");
        values.put("AuthController#wechat", "微信小程序登录");
        values.put("AuthController#me", "获取当前登录用户");
        values.put("AuthController#logout", "退出当前登录");
        values.put("AdminCaseController#list", "分页查询后台报单");
        values.put("AdminCaseController#detail", "查询后台报单详情");
        values.put("AdminCaseController#assign", "分配报单经办人");
        values.put("AdminCaseController#status", "变更报单状态");
        values.put("AdminCaseController#review", "审核单项资料");
        values.put("AdminCaseController#supplement", "发起客户补件");
        values.put("CaseController#create", "创建客户报单草稿");
        values.put("CaseController#mine", "分页查询我的报单");
        values.put("CaseController#detail", "查询我的报单详情");
        values.put("CaseController#invoice", "保存开票信息");
        values.put("CaseController#simple", "保存知识产权或资质简表");
        values.put("CaseController#text", "保存文字类资料");
        values.put("CaseController#submit", "提交或重新提交报单");
        values.put("CaseController#timeline", "查询报单进度时间线");
        values.put("AdminProductController#list", "查询后台产品列表");
        values.put("AdminProductController#create", "创建服务产品");
        values.put("AdminProductController#update", "修改服务产品");
        values.put("AdminProductController#publish", "发布服务产品");
        values.put("AdminProductController#unpublish", "下架服务产品");
        values.put("AdminProductController#createVersion", "创建资料模板版本");
        values.put("AdminProductController#publishTemplate", "发布资料模板版本");
        values.put("CatalogController#catalog", "查询小程序服务目录");
        values.put("CatalogController#template", "查询产品资料模板");
        values.put("AdminCompanyController#list", "分页查询企业客户");
        values.put("AdminCompanyController#create", "创建企业客户");
        values.put("AdminCompanyController#addMember", "添加或更新企业成员");
        values.put("CompanyController#mine", "查询当前用户所属企业");
        values.put("CompanyController#detail", "查询当前用户企业详情");
        values.put("DashboardController#dashboard", "查询后台工作台汇总");
        values.put("ExportController#excel", "创建审核表 Excel 导出任务");
        values.put("ExportController#zip", "创建贸易增量 ZIP 导出任务");
        values.put("ExportController#get", "查询导出任务");
        values.put("ExportController#download", "获取导出文件下载地址");
        values.put("FileController#upload", "上传报单资料文件");
        values.put("FileController#delete", "删除报单资料文件");
        values.put("FileController#preview", "获取文件受控预览地址");
        values.put("FileController#list", "查询资料项文件列表");
        values.put("ImportController#template", "获取企业导入模板");
        values.put("ImportController#create", "创建企业 Excel 导入任务");
        values.put("ImportController#get", "查询企业导入任务");
        values.put("ImportController#error", "获取导入错误报告");
        values.put("NotificationController#mine", "分页查询我的通知");
        values.put("NotificationController#read", "标记通知为已读");
        return Map.copyOf(values);
    }
}
