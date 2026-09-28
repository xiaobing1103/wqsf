package com.wqst.api.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.wqst.api.common.BusinessException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileInspectorTest {
    private final FileInspector inspector=new FileInspector();
    @TempDir Path temp;

    @Test void acceptsPngOnlyWhenExtensionMimeAndHeaderAgree(){byte[] png={(byte)0x89,0x50,0x4e,0x47,0x0d,0x0a,0x1a,0x0a};FileInspector.Inspection result=inspector.validate("执照.png","image/png",png.length,png,Set.of("png"),100);assertThat(result.extension()).isEqualTo("png");}
    @Test void rejectsRenamedExecutableAndOversize(){byte[] mz={'M','Z',0,0};assertThatThrownBy(()->inspector.validate("执照.png","image/png",4,mz,Set.of("png"),100)).isInstanceOf(BusinessException.class).extracting("code").isEqualTo("FILE_SIGNATURE_MISMATCH");assertThatThrownBy(()->inspector.validate("执照.png","image/png",101,new byte[8],Set.of("png"),100)).isInstanceOf(BusinessException.class).extracting("code").isEqualTo("FILE_TOO_LARGE");}
    @Test void validatesRealXlsxContainerEntries()throws Exception{Path valid=temp.resolve("valid.xlsx");try(ZipOutputStream out=new ZipOutputStream(Files.newOutputStream(valid))){out.putNextEntry(new ZipEntry("[Content_Types].xml"));out.write("x".getBytes());out.closeEntry();out.putNextEntry(new ZipEntry("xl/workbook.xml"));out.write("x".getBytes());out.closeEntry();}inspector.validateXlsx(valid);Path invalid=temp.resolve("invalid.xlsx");Files.write(invalid,new byte[]{'P','K',3,4});assertThatThrownBy(()->inspector.validateXlsx(invalid)).isInstanceOf(BusinessException.class);}
    @Test void recognizesAllSupportedMagicHeaders(){assertThat(inspector.validate("a.jpg","image/jpeg",3,new byte[]{(byte)0xff,(byte)0xd8,(byte)0xff},Set.of("jpg"),20).extension()).isEqualTo("jpg");assertThat(inspector.validate("a.pdf","application/pdf",5,"%PDF-".getBytes(),Set.of("pdf"),20).extension()).isEqualTo("pdf");byte[] mp4=new byte[12];System.arraycopy("ftyp".getBytes(),0,mp4,4,4);assertThat(inspector.validate("a.mp4","video/mp4",12,mp4,Set.of("mp4"),20).extension()).isEqualTo("mp4");assertThat(inspector.validate("a.xlsx","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",4,new byte[]{'P','K',3,4},Set.of("xlsx"),20).extension()).isEqualTo("xlsx");}
    @Test void rejectsMissingExtensionEmptyAndMimeMismatch(){assertThatThrownBy(()->inspector.validate("file","image/png",8,new byte[8],Set.of("png"),100)).isInstanceOf(BusinessException.class).extracting("code").isEqualTo("FILE_EXTENSION_DENIED");assertThatThrownBy(()->inspector.validate("a.png","image/png",0,new byte[8],Set.of("png"),100)).isInstanceOf(BusinessException.class).extracting("code").isEqualTo("FILE_EMPTY");assertThatThrownBy(()->inspector.validate("a.png","application/pdf",8,new byte[8],Set.of("png"),100)).isInstanceOf(BusinessException.class).extracting("code").isEqualTo("FILE_MIME_MISMATCH");}
    @Test void rejectsShortOrCorruptHeadersForEverySupportedType(){assertSignatureRejected("a.jpg","image/jpeg",new byte[]{(byte)0xff},"jpg");assertSignatureRejected("a.jpeg","image/jpeg",new byte[]{(byte)0xff,(byte)0xd8,0},"jpeg");assertSignatureRejected("a.png","image/png",new byte[]{(byte)0x89,0x50},"png");assertSignatureRejected("a.pdf","application/pdf","%PDX-".getBytes(),"pdf");assertSignatureRejected("a.mp4","video/mp4",new byte[8],"mp4");byte[] corruptMp4=new byte[12];System.arraycopy("xxxx".getBytes(),0,corruptMp4,4,4);assertSignatureRejected("a.mp4","video/mp4",corruptMp4,"mp4");assertSignatureRejected("a.xlsx","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",new byte[]{'P','K',1,2},"xlsx");}
    private void assertSignatureRejected(String name,String mime,byte[] header,String extension){assertThatThrownBy(()->inspector.validate(name,mime,Math.max(1,header.length),header,Set.of(extension),100)).isInstanceOf(BusinessException.class).extracting("code").isEqualTo("FILE_SIGNATURE_MISMATCH");}
}
