package com.wqst.api.common;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
class SafeFilesTest {
    @Test void removesTraversalAndReservedCharacters(){String entry=SafeFiles.zipEntry("01_营业执照","../../征信:报告?.pdf");assertThat(entry).doesNotContain("..").doesNotContain(":").doesNotContain("?").doesNotContain("\\");assertThat(entry).startsWith("01_营业执照/");}
    @Test void extractsOnlySafeExtension(){assertThat(SafeFiles.extension("C:\\fake\\report.PDF")).isEqualTo("pdf");assertThat(SafeFiles.extension("no-extension")).isEmpty();}
    @Test void normalizesBlankDotAndLongNames(){assertThat(SafeFiles.fileName(null)).isEqualTo("file");assertThat(SafeFiles.fileName("..")).isEqualTo("file");assertThat(SafeFiles.fileName("a".repeat(200))).hasSize(120);assertThat(SafeFiles.extension(null)).isEmpty();}
}
