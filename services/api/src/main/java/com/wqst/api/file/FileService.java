package com.wqst.api.file;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wqst.api.audit.AuditService;
import com.wqst.api.caseorder.CaseService;
import com.wqst.api.caseorder.entity.MaterialSubmissionEntity;
import com.wqst.api.caseorder.entity.ServiceCaseEntity;
import com.wqst.api.caseorder.mapper.MaterialSubmissionMapper;
import com.wqst.api.catalog.entity.MaterialTemplateItemEntity;
import com.wqst.api.catalog.mapper.MaterialTemplateItemMapper;
import com.wqst.api.common.BusinessException;
import com.wqst.api.common.SafeFiles;
import com.wqst.api.common.SecuritySupport;
import com.wqst.api.company.CompanyService;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileService {
    private final FileObjectMapper files;private final MaterialTemplateItemMapper templateItems;private final MaterialSubmissionMapper materials;
    private final ObjectStorageService storage;private final FileInspector inspector;private final CaseService cases;private final CompanyService companies;
    private final SecuritySupport security;private final AuditService audit;
    public FileService(FileObjectMapper files,MaterialTemplateItemMapper templateItems,MaterialSubmissionMapper materials,ObjectStorageService storage,FileInspector inspector,CaseService cases,CompanyService companies,SecuritySupport security,AuditService audit){this.files=files;this.templateItems=templateItems;this.materials=materials;this.storage=storage;this.inspector=inspector;this.cases=cases;this.companies=companies;this.security=security;this.audit=audit;}

    @Transactional
    public FileView upload(long caseId,long materialId,MultipartFile upload,long userId){
        ServiceCaseEntity c=cases.require(caseId);companies.requireClientAccess(c.getCompanyId(),userId);if(!List.of("DRAFT","NEED_SUPPLEMENT").contains(c.getStatus()))throw new BusinessException("CASE_NOT_EDITABLE","当前状态不允许上传文件",HttpStatus.CONFLICT);
        MaterialSubmissionEntity material=cases.requireMaterial(materialId);if(!Objects.equals(material.getCaseId(),caseId)||!"FILE".equals(material.getInputType()))throw new BusinessException("MATERIAL_INPUT_INVALID","文件与资料项不匹配",HttpStatus.BAD_REQUEST);
        MaterialTemplateItemEntity template=templateItems.selectById(material.getTemplateItemId());if(template==null)throw new BusinessException("TEMPLATE_ITEM_NOT_FOUND","资料模板项不存在",HttpStatus.CONFLICT);
        long existing=files.selectCount(new LambdaQueryWrapper<FileObjectEntity>().eq(FileObjectEntity::getMaterialId,materialId).eq(FileObjectEntity::getStatus,"READY"));
        if(existing>=template.getMaxFileCount())throw new BusinessException("FILE_COUNT_EXCEEDED","上传文件数量超过限制",HttpStatus.CONFLICT);
        Path staged=null;String objectKey=null;
        try{
            staged=Files.createTempFile("wqst-upload-",".tmp");upload.transferTo(staged);byte[] header=readHeader(staged);
            Set<String> allowed=Set.copyOf(Arrays.asList(template.getAllowedExtensions().toLowerCase().split(",")));
            FileInspector.Inspection checked=inspector.validate(upload.getOriginalFilename(),upload.getContentType(),Files.size(staged),header,allowed,(long)template.getMaxFileSizeMb()*1024*1024);
            String uuid=UUID.randomUUID().toString();objectKey="cases/"+caseId+"/"+materialId+"/"+uuid+"."+checked.extension();storage.put(objectKey,staged,checked.contentType());storage.deleteOnRollback(objectKey);
            FileObjectEntity entity=new FileObjectEntity();entity.setCaseId(caseId);entity.setBucketName(storage.bucket());entity.setMaterialId(materialId);entity.setOriginalName(SafeFiles.fileName(upload.getOriginalFilename()));entity.setStoredName(uuid+"."+checked.extension());entity.setObjectKey(objectKey);entity.setContentType(checked.contentType());entity.setExtension(checked.extension());entity.setFileSize(Files.size(staged));entity.setSha256(sha256(staged));entity.setStatus("READY");entity.setIsSensitive(material.getIsSensitive());entity.setCreatedBy(userId);entity.setUploadedBy(userId);files.insert(entity);cases.markFileSubmitted(caseId,materialId);audit.operation(userId,"FILE_UPLOAD","FILE",entity.getId(),"上传报单资料文件");return view(entity);
        }catch(BusinessException ex){if(objectKey!=null)storage.deleteQuietly(objectKey);throw ex;}catch(Exception ex){if(objectKey!=null)storage.deleteQuietly(objectKey);throw new BusinessException("FILE_UPLOAD_FAILED","文件上传失败",HttpStatus.INTERNAL_SERVER_ERROR);}finally{if(staged!=null)try{Files.deleteIfExists(staged);}catch(Exception ignored){}}
    }

    @Transactional
    public void delete(long fileId,long userId){FileObjectEntity f=require(fileId);if(f.getCaseId()==null)throw new BusinessException("FILE_DELETE_DENIED","无权删除系统文件",HttpStatus.FORBIDDEN);ServiceCaseEntity c=cases.require(f.getCaseId());companies.requireClientAccess(c.getCompanyId(),userId);if(!Objects.equals(f.getUploadedBy(),userId))throw new BusinessException("FILE_DELETE_DENIED","只能删除自己上传的文件",HttpStatus.FORBIDDEN);if(!List.of("DRAFT","NEED_SUPPLEMENT").contains(c.getStatus()))throw new BusinessException("CASE_NOT_EDITABLE","当前状态不允许删除文件",HttpStatus.CONFLICT);f.setStatus("DELETED");files.updateById(f);long remaining=files.selectCount(new LambdaQueryWrapper<FileObjectEntity>().eq(FileObjectEntity::getMaterialId,f.getMaterialId()).eq(FileObjectEntity::getStatus,"READY"));if(remaining==0){MaterialSubmissionEntity m=materials.selectById(f.getMaterialId());m.setSubmitStatus("NOT_SUBMITTED");materials.updateById(m);}audit.operation(userId,"FILE_DELETE","FILE",fileId,"删除报单资料文件");storage.deleteAfterCommit(f.getObjectKey());}

    public PreviewView preview(long fileId){FileObjectEntity f=require(fileId);long operatorId;if(security.isAdmin()){operatorId=security.adminId();try{security.permission("case:view");if(Boolean.TRUE.equals(f.getIsSensitive()))security.permission("file:sensitive:view");}catch(RuntimeException ex){audit.file(operatorId,fileId,f.getCaseId(),"PREVIEW","DENIED");throw ex;}}else{operatorId=security.clientId();if(f.getCaseId()==null){audit.file(operatorId,fileId,null,"PREVIEW","DENIED");throw new BusinessException("FILE_SCOPE_DENIED","无权访问该文件",HttpStatus.FORBIDDEN);}ServiceCaseEntity c=cases.require(f.getCaseId());try{companies.requireClientAccess(c.getCompanyId(),operatorId);}catch(RuntimeException ex){audit.file(operatorId,fileId,f.getCaseId(),"PREVIEW","DENIED");throw ex;}}String url=storage.presign(f.getObjectKey(),Duration.ofMinutes(5));audit.file(operatorId,fileId,f.getCaseId(),"PREVIEW","SUCCESS");return new PreviewView(url,OffsetDateTime.now().plusMinutes(5));}
    public List<FileView> list(long materialId){MaterialSubmissionEntity m=cases.requireMaterial(materialId);ServiceCaseEntity c=cases.require(m.getCaseId());if(security.isAdmin()){security.adminId();security.permission("case:view");}else companies.requireClientAccess(c.getCompanyId(),security.clientId());return files.selectList(new LambdaQueryWrapper<FileObjectEntity>().eq(FileObjectEntity::getMaterialId,materialId).eq(FileObjectEntity::getStatus,"READY").orderByAsc(FileObjectEntity::getCreatedAt)).stream().map(this::view).toList();}
    public FileObjectEntity require(long id){FileObjectEntity f=files.selectById(id);if(f==null||!"READY".equals(f.getStatus()))throw new BusinessException("FILE_NOT_FOUND","文件不存在",HttpStatus.NOT_FOUND);return f;}
    public List<FileObjectEntity> readyByCase(long caseId){return files.selectList(new LambdaQueryWrapper<FileObjectEntity>().eq(FileObjectEntity::getCaseId,caseId).eq(FileObjectEntity::getStatus,"READY").orderByAsc(FileObjectEntity::getMaterialId).orderByAsc(FileObjectEntity::getId));}
    public FileObjectEntity saveSystemFile(String objectKey,String originalName,String contentType,String extension,long size,String sha,Long operatorId){return saveSystemFile(objectKey,originalName,contentType,extension,size,sha,operatorId,false);}
    public FileObjectEntity saveSystemFile(String objectKey,String originalName,String contentType,String extension,long size,String sha,Long operatorId,boolean sensitive){FileObjectEntity f=new FileObjectEntity();f.setBucketName(storage.bucket());f.setOriginalName(SafeFiles.fileName(originalName));f.setStoredName(objectKey.substring(objectKey.lastIndexOf('/')+1));f.setObjectKey(objectKey);f.setContentType(contentType);f.setExtension(extension);f.setFileSize(size);f.setSha256(sha);f.setStatus("READY");f.setIsSensitive(sensitive);f.setCreatedBy(operatorId);f.setUploadedBy(operatorId);files.insert(f);return f;}
    private FileView view(FileObjectEntity f){return new FileView(f.getId(),f.getCaseId(),f.getMaterialId(),f.getOriginalName(),f.getContentType(),f.getExtension(),f.getFileSize(),f.getSha256(),Boolean.TRUE.equals(f.getIsSensitive()),f.getStatus(),f.getCreatedAt());}
    private byte[] readHeader(Path path)throws Exception{try(InputStream in=Files.newInputStream(path)){return in.readNBytes(16);}}
    private String sha256(Path path)throws Exception{MessageDigest md=MessageDigest.getInstance("SHA-256");try(InputStream raw=Files.newInputStream(path);DigestInputStream in=new DigestInputStream(raw,md)){in.transferTo(OutputStream.nullOutputStream());}return HexFormat.of().formatHex(md.digest());}
    public record FileView(Long id,Long caseId,Long materialId,String originalName,String contentType,String extension,Long fileSize,String sha256,boolean sensitive,String status,java.time.LocalDateTime createdAt){}
    public record PreviewView(String previewUrl,OffsetDateTime expiresAt){}
}
