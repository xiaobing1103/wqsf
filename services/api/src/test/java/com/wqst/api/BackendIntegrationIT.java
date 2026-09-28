package com.wqst.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.alibaba.excel.EasyExcel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wqst.api.imports.ImportService;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "local"})
@Testcontainers(disabledWithoutDocker = true)
@TestMethodOrder(OrderAnnotation.class)
class BackendIntegrationIT {
    private static final String ADMIN_USERNAME = "integration_admin";
    private static final String ADMIN_PASSWORD = "Integration-Only-Password-2026";

    @Container
    static final MySQLContainer<?> MYSQL = mysqlContainer();

    @Container
    static final GenericContainer<?> VALKEY = new GenericContainer<>("valkey/valkey:8")
            .withExposedPorts(6379)
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)));

    @Container
    static final GenericContainer<?> SEAWEEDFS = new GenericContainer<>("chrislusf/seaweedfs:4.06")
            .withExposedPorts(8333, 9333, 8888)
            .withCopyFileToContainer(MountableFile.forClasspathResource("seaweedfs-s3-test.json"), "/etc/seaweedfs/s3.json")
            .withCommand("server", "-s3", "-s3.config=/etc/seaweedfs/s3.json", "-dir=/data")
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(3)));

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.data.redis.host", VALKEY::getHost);
        registry.add("spring.data.redis.port", () -> VALKEY.getMappedPort(6379));
        registry.add("wqst.s3.endpoint", () -> "http://" + SEAWEEDFS.getHost() + ":" + SEAWEEDFS.getMappedPort(8333));
        registry.add("wqst.s3.access-key", () -> "wqst_it_access");
        registry.add("wqst.s3.secret-key", () -> "wqst_it_secret");
        registry.add("wqst.s3.bucket", () -> "wqst-private-it");
        registry.add("wqst.s3.region", () -> "us-east-1");
        registry.add("wqst.security.encryption-key", () -> "integration-only-encryption-key-2026");
        registry.add("wqst.bootstrap.admin-username", () -> ADMIN_USERNAME);
        registry.add("wqst.bootstrap.admin-password", () -> ADMIN_PASSWORD);
        registry.add("wqst.auth.dev-client-login-enabled", () -> true);
        registry.add("wqst.export.recovery-delay-ms", () -> 3_600_000);
        registry.add("wqst.import.recovery-delay-ms", () -> 3_600_000);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;

    @Test
    @Order(1)
    void databaseScriptsSeedsConstraintsAndRollbackWork() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='wqst'", Integer.class)).isEqualTo(26);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM service_module", Integer.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM service_product", Integer.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM material_template", Integer.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM material_template_item", Integer.class)).isEqualTo(17);

        String rollbackCode = "ROLLBACK_" + UUID.randomUUID().toString().substring(0, 8);
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            jdbc.update("INSERT INTO service_module(module_code,module_name,sort_order,status) VALUES (?,?,?,?)",
                    rollbackCode, "回滚验证", 99, "ENABLED");
            status.setRollbackOnly();
        });
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM service_module WHERE module_code=?", Integer.class, rollbackCode)).isZero();
        assertThatThrownBy(() -> jdbc.update("INSERT INTO service_module(module_code,module_name,sort_order,status) VALUES (?,?,?,?)",
                "TRADE_INCREMENT", "重复模块", 99, "ENABLED")).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Order(2)
    void completeDraftSupplementImportAndExportWorkflow() throws Exception {
        String clientToken = token(postJson("/api/v1/auth/client/dev-login", "{}", null, null));
        JsonNode company = data(getJson("/api/v1/companies/my", clientToken)).get(0);
        JsonNode catalog = data(getJson("/api/v1/services/catalog", null));
        JsonNode trade = findBy(catalog, "moduleCode", "TRADE_INCREMENT");
        long productId = trade.path("products").get(0).path("id").asLong();

        String createBody = """
                {"companyId":%d,"productId":%d,"serviceType":"TRADE_INCREMENT","caseMonth":"2026-09"}
                """.formatted(company.path("id").asLong(), productId);
        JsonNode created = data(postJson("/api/v1/cases", createBody, clientToken, "it-create-case"));
        long caseId = created.path("id").asLong();
        assertThat(created.path("materials")).hasSize(11);

        String invoice = """
                {"companyName":"集成测试企业","taxNo":"91430100INTEGRATION1","contactAddress":"测试地址",
                 "contactPhone":"13800000000","legalPerson":"测试法人","companyEmail":"it@example.invalid",
                 "bankBranch":"测试银行网点","basicAccount":"6222021234567890"}
                """;
        putJson("/api/v1/cases/" + caseId + "/invoice-info", invoice, clientToken);

        long supplementedMaterialId = 0;
        long supplementedFileId = 0;
        for (JsonNode material : created.path("materials")) {
            if ("FILE".equals(material.path("inputType").asText())) {
                JsonNode uploaded = upload(caseId, material.path("id").asLong(), clientToken);
                if (supplementedMaterialId == 0) {
                    supplementedMaterialId = material.path("id").asLong();
                    supplementedFileId = uploaded.path("id").asLong();
                }
            } else if ("URL".equals(material.path("inputType").asText())) {
                putJson("/api/v1/cases/" + caseId + "/materials/" + material.path("id").asLong() + "/text",
                        "{\"value\":\"https://example.invalid/report\"}", clientToken);
            }
        }
        data(postJson("/api/v1/cases/" + caseId + "/submit", "", clientToken, "it-submit-first"));

        String adminToken = token(postJson("/api/v1/auth/admin/login",
                "{\"username\":\"" + ADMIN_USERNAME + "\",\"password\":\"" + ADMIN_PASSWORD + "\"}", null, null));
        data(postJson("/api/v1/admin/cases/" + caseId + "/supplement-request",
                "{\"materialIds\":[" + supplementedMaterialId + "],\"clientMessage\":\"请重新上传清晰文件\"}",
                adminToken, "it-supplement"));

        mvc.perform(delete("/api/v1/files/{fileId}", supplementedFileId).header("Authorization", bearer(clientToken)))
                .andExpect(status().isOk());
        upload(caseId, supplementedMaterialId, clientToken);
        data(postJson("/api/v1/cases/" + caseId + "/submit", "", clientToken, "it-submit-second"));
        data(postJson("/api/v1/admin/cases/" + caseId + "/status", "{\"targetStatus\":\"PROCESSING\",\"note\":\"开始办理\"}", adminToken, null));
        JsonNode completed = data(postJson("/api/v1/admin/cases/" + caseId + "/status", "{\"targetStatus\":\"COMPLETED\",\"note\":\"办理完成\"}", adminToken, null));
        assertThat(completed.path("status").asText()).isEqualTo("COMPLETED");

        verifyCrossCompanyAccessDenied(adminToken, clientToken, caseId);
        verifyMixedExcelImport(adminToken);

        long excelJob = data(postJson("/api/v1/admin/exports/review-excel", "{\"caseIds\":[" + caseId + "]}", adminToken, "it-export-excel")).path("id").asLong();
        long zipJob = data(postJson("/api/v1/admin/exports/material-zip", "{\"caseId\":" + caseId + ",\"includeSensitive\":true}", adminToken, "it-export-zip")).path("id").asLong();
        awaitJob("/api/v1/admin/exports/" + excelJob, adminToken, 20_000);
        awaitJob("/api/v1/admin/exports/" + zipJob, adminToken, 20_000);
        JsonNode download = data(postJson("/api/v1/admin/exports/" + zipJob + "/download", "", adminToken, null));
        assertThat(download.path("downloadUrl").asText()).startsWith("http");
    }

    private void verifyCrossCompanyAccessDenied(String adminToken, String ownerToken, long caseId) throws Exception {
        JsonNode account = data(postJson("/api/v1/admin/accounts",
                "{\"accountType\":\"CLIENT\",\"displayName\":\"隔离测试客户\",\"phone\":\"13900000000\",\"roles\":[\"CLIENT\"]}", adminToken, null));
        String otherToken = token(postJson("/api/v1/auth/client/dev-login", "{\"userId\":" + account.path("id").asLong() + "}", null, null));
        mvc.perform(get("/api/v1/cases/{caseId}", caseId).header("Authorization", bearer(otherToken)))
                .andExpect(status().isForbidden());
        assertThat(data(getJson("/api/v1/cases/" + caseId, ownerToken)).path("id").asLong()).isEqualTo(caseId);
    }

    private void verifyMixedExcelImport(String adminToken) throws Exception {
        byte[] workbook;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            List<List<String>> head = ImportService.HEADERS.stream().map(List::of).toList();
            List<List<String>> rows = List.of(
                    List.of("导入测试企业", "91430100IMPORT0001", "联系人", "13700000000", "湖南省", "衡阳市", "测试法人", "import@example.invalid", "测试地址"),
                    List.of("导入测试企业", "91430100IMPORT0001", "联系人", "13700000000", "湖南省", "衡阳市", "测试法人", "import@example.invalid", "测试地址"),
                    List.of("错误企业", "BAD", "联系人", "BAD", "湖南省", "衡阳市", "测试法人", "bad@example.invalid", "测试地址"));
            EasyExcel.write(out).head(head).sheet("客户导入").doWrite(rows);
            workbook = out.toByteArray();
        }
        MockMultipartFile file = new MockMultipartFile("file", "客户导入.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", workbook);
        MvcResult result = mvc.perform(multipart("/api/v1/admin/imports").file(file).param("dryRun", "false")
                        .header("Authorization", bearer(adminToken)).header("Idempotency-Key", "it-company-import"))
                .andExpect(status().isOk()).andReturn();
        long jobId = data(parse(result)).path("id").asLong();
        JsonNode job = awaitJob("/api/v1/admin/imports/" + jobId, adminToken, 20_000);
        assertThat(job.path("successRows").asInt()).isEqualTo(1);
        assertThat(job.path("skippedRows").asInt()).isEqualTo(1);
        assertThat(job.path("failedRows").asInt()).isEqualTo(1);
    }

    private JsonNode awaitJob(String path, String token, long timeoutMs) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutMs;
        JsonNode job;
        do {
            job = data(getJson(path, token));
            if (List.of("SUCCEEDED", "FAILED").contains(job.path("status").asText())) break;
            Thread.sleep(100);
        } while (System.currentTimeMillis() < deadline);
        assertThat(job.path("status").asText()).isEqualTo("SUCCEEDED");
        return job;
    }

    private JsonNode upload(long caseId, long materialId, String token) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "资料.jpg", "image/jpeg",
                new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0x00});
        MvcResult result = mvc.perform(multipart("/api/v1/files").file(file)
                        .param("caseId", Long.toString(caseId)).param("materialId", Long.toString(materialId))
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        return data(parse(result));
    }

    private JsonNode getJson(String path, String token) throws Exception {
        var request = get(path);
        if (token != null) request.header("Authorization", bearer(token));
        return parse(mvc.perform(request).andExpect(status().isOk()).andReturn());
    }

    private JsonNode putJson(String path, String body, String token) throws Exception {
        return parse(mvc.perform(put(path).contentType(MediaType.APPLICATION_JSON).content(body)
                .header("Authorization", bearer(token))).andExpect(status().isOk()).andReturn());
    }

    private JsonNode postJson(String path, String body, String token, String idempotencyKey) throws Exception {
        var request = post(path).contentType(MediaType.APPLICATION_JSON);
        if (body != null && !body.isEmpty()) request.content(body);
        if (token != null) request.header("Authorization", bearer(token));
        if (idempotencyKey != null) request.header("Idempotency-Key", idempotencyKey);
        return parse(mvc.perform(request).andExpect(status().isOk()).andReturn());
    }

    private JsonNode parse(MvcResult result) throws Exception { return json.readTree(result.getResponse().getContentAsByteArray()); }
    private JsonNode data(JsonNode response) { assertThat(response.path("success").asBoolean()).isTrue(); return response.path("data"); }
    private String token(JsonNode response) { return data(response).path("token").asText(); }
    private String bearer(String token) { return "Bearer " + token; }
    private JsonNode findBy(JsonNode array, String field, String value) {
        for (JsonNode item : array) if (value.equals(item.path(field).asText())) return item;
        throw new AssertionError("未找到 " + field + "=" + value);
    }

    private static MySQLContainer<?> mysqlContainer() {
        return new MySQLContainer<>("mysql:8.4")
                .withDatabaseName("wqst")
                .withUsername("wqst")
                .withPassword("wqst_it_password")
                .withCopyFileToContainer(MountableFile.forHostPath(projectFile("database", "001_init.sql")), "/docker-entrypoint-initdb.d/001_init.sql")
                .withCopyFileToContainer(MountableFile.forHostPath(projectFile("database", "002_full_schema.sql")), "/docker-entrypoint-initdb.d/002_full_schema.sql");
    }

    private static String projectFile(String directory, String name) {
        Path cursor = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (cursor != null) {
            Path candidate = cursor.resolve(directory).resolve(name);
            if (Files.isRegularFile(candidate)) return candidate.toString();
            cursor = cursor.getParent();
        }
        throw new IllegalStateException("找不到项目文件: " + directory + "/" + name);
    }
}
