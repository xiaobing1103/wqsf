package com.wqst.api.common;

import static org.assertj.core.api.Assertions.assertThat;
import com.wqst.api.config.AppProperties;
import org.junit.jupiter.api.Test;

class CryptoServiceTest {
    private final CryptoService crypto=new CryptoService(new AppProperties(
            new AppProperties.Security("test-key-with-enough-entropy-123456"),
            new AppProperties.Bootstrap("",""),new AppProperties.Wechat("",""),
            new AppProperties.S3("http://localhost","a","b","c","us-east-1"),
            new AppProperties.Export(30,2),new AppProperties.Auth(true)));

    @Test void encryptsRandomlyAndDecrypts(){String first=crypto.encrypt("13800000000");String second=crypto.encrypt("13800000000");assertThat(first).isNotEqualTo(second);assertThat(crypto.decrypt(first)).isEqualTo("13800000000");assertThat(crypto.hash("13800000000")).isEqualTo(crypto.hash("13800000000"));}
    @Test void masksSensitiveValues(){assertThat(crypto.maskPhone("13800000000")).isEqualTo("138 **** 0000");assertThat(crypto.maskAccount("6222021234567890")).isEqualTo("********7890");assertThat(crypto.maskTaxNo("91430100123456789X")).isEqualTo("9143**********9X");}
    @Test void handlesBlankAndShortValues(){assertThat(crypto.encrypt(" ")).isNull();assertThat(crypto.decrypt(null)).isNull();assertThat(crypto.hash("")).isNull();assertThat(crypto.maskPhone("123")).isEqualTo("****");assertThat(crypto.maskTaxNo("123")).isEqualTo("******");assertThat(crypto.maskAccount(null)).isNull();}
}
