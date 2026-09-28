package com.wqst.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

class ProdConfigurationTest {
    @Test
    void productionYamlLoadsWithoutDuplicateKeysAndDisablesDevLogin() throws Exception {
        List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                .load("application-prod", new ClassPathResource("application-prod.yml"));

        assertThat(sources).hasSize(1);
        assertThat(sources.getFirst().getProperty("wqst.auth.dev-client-login-enabled")).isEqualTo(false);
        assertThat(sources.getFirst().getProperty("wqst.security.encryption-key"))
                .isEqualTo("${DATA_ENCRYPTION_KEY}");
        assertThat(sources.getFirst().getProperty("spring.datasource.password"))
                .isEqualTo("${MYSQL_PASSWORD}");
    }
}
