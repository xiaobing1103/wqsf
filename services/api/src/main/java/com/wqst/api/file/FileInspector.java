package com.wqst.api.file;

import com.wqst.api.common.BusinessException;
import com.wqst.api.common.SafeFiles;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.nio.file.Path;
import java.util.zip.ZipFile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class FileInspector {
    private static final Map<String,Set<String>> MIMES=Map.of(
            "jpg",Set.of("image/jpeg"),"jpeg",Set.of("image/jpeg"),"png",Set.of("image/png"),
            "pdf",Set.of("application/pdf"),"mp4",Set.of("video/mp4"),
            "xlsx",Set.of("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

    public Inspection validate(String originalName,String contentType,long size,byte[] header,Set<String> allowed,long maxBytes){
        String extension=SafeFiles.extension(originalName);
        if(extension.isBlank()||!allowed.contains(extension))throw new BusinessException("FILE_EXTENSION_DENIED","不支持该文件扩展名",HttpStatus.BAD_REQUEST);
        if(size<=0)throw new BusinessException("FILE_EMPTY","文件不能为空",HttpStatus.BAD_REQUEST);
        if(size>maxBytes)throw new BusinessException("FILE_TOO_LARGE","文件大小超过限制",HttpStatus.PAYLOAD_TOO_LARGE);
        String mime=contentType==null?"":contentType.toLowerCase(Locale.ROOT).split(";")[0].trim();
        if(!MIMES.getOrDefault(extension,Set.of()).contains(mime))throw new BusinessException("FILE_MIME_MISMATCH","文件 MIME 类型与扩展名不一致",HttpStatus.BAD_REQUEST);
        if(!magicMatches(extension,header))throw new BusinessException("FILE_SIGNATURE_MISMATCH","文件内容与扩展名不一致",HttpStatus.BAD_REQUEST);
        return new Inspection(extension,mime);
    }

    private boolean magicMatches(String ext,byte[] bytes){
        if(bytes==null)return false;
        return switch(ext){
            case "jpg","jpeg"->bytes.length>=3&&(bytes[0]&255)==0xff&&(bytes[1]&255)==0xd8&&(bytes[2]&255)==0xff;
            case "png"->bytes.length>=8&&Arrays.equals(Arrays.copyOf(bytes,8),new byte[]{(byte)0x89,0x50,0x4e,0x47,0x0d,0x0a,0x1a,0x0a});
            case "pdf"->starts(bytes,"%PDF-");
            case "mp4"->bytes.length>=12&&new String(bytes,4,4,StandardCharsets.US_ASCII).equals("ftyp");
            case "xlsx"->bytes.length>=4&&bytes[0]=='P'&&bytes[1]=='K'&&(bytes[2]==3||bytes[2]==5||bytes[2]==7)&&(bytes[3]==4||bytes[3]==6||bytes[3]==8);
            default->false;
        };
    }
    private boolean starts(byte[] bytes,String value){byte[] expected=value.getBytes(StandardCharsets.US_ASCII);if(bytes.length<expected.length)return false;for(int i=0;i<expected.length;i++)if(bytes[i]!=expected[i])return false;return true;}
    public void validateXlsx(Path path){try(ZipFile zip=new ZipFile(path.toFile())){if(zip.getEntry("[Content_Types].xml")==null||zip.getEntry("xl/workbook.xml")==null)throw new BusinessException("FILE_SIGNATURE_MISMATCH","文件不是有效的 Excel 工作簿",HttpStatus.BAD_REQUEST);}catch(BusinessException ex){throw ex;}catch(Exception ex){throw new BusinessException("FILE_SIGNATURE_MISMATCH","文件不是有效的 Excel 工作簿",HttpStatus.BAD_REQUEST);}}
    public record Inspection(String extension,String contentType){}
}
