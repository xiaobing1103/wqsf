package com.wqst.api.file;

import com.wqst.api.common.BusinessException;
import com.wqst.api.config.AppProperties;
import java.nio.file.Path;
import java.time.Duration;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class ObjectStorageService {
    private final S3Client s3; private final S3Presigner presigner; private final AppProperties props;
    public ObjectStorageService(S3Client s3,S3Presigner presigner,AppProperties props){this.s3=s3;this.presigner=presigner;this.props=props;}
    public String bucket(){return props.s3().bucket();}
    public void healthCheck(){ensureBucket();s3.headBucket(HeadBucketRequest.builder().bucket(bucket()).build());}
    public void put(String key,Path path,String contentType){try{ensureBucket();s3.putObject(PutObjectRequest.builder().bucket(bucket()).key(key).contentType(contentType).build(),RequestBody.fromFile(path));}catch(RuntimeException ex){throw unavailable("对象存储上传失败",ex);}}
    public void put(String key,byte[] bytes,String contentType){try{ensureBucket();s3.putObject(PutObjectRequest.builder().bucket(bucket()).key(key).contentType(contentType).build(),RequestBody.fromBytes(bytes));}catch(RuntimeException ex){throw unavailable("对象存储上传失败",ex);}}
    public byte[] get(String key){try{ResponseBytes<GetObjectResponse> bytes=s3.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket()).key(key).build());return bytes.asByteArray();}catch(RuntimeException ex){throw unavailable("对象存储读取失败",ex);}}
    public void delete(String key){try{s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket()).key(key).build());}catch(RuntimeException ex){throw unavailable("对象存储删除失败",ex);}}
    public void deleteQuietly(String key){try{delete(key);}catch(RuntimeException ignored){}}
    public void deleteOnRollback(String key){if(!TransactionSynchronizationManager.isSynchronizationActive())return;TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCompletion(int status){if(status!=STATUS_COMMITTED)deleteQuietly(key);}});}
    public void deleteAfterCommit(String key){if(!TransactionSynchronizationManager.isSynchronizationActive()){deleteQuietly(key);return;}TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCommit(){deleteQuietly(key);}});}
    public String presign(String key,Duration duration){try{return presigner.presignGetObject(GetObjectPresignRequest.builder().signatureDuration(duration).getObjectRequest(GetObjectRequest.builder().bucket(bucket()).key(key).build()).build()).url().toString();}catch(RuntimeException ex){throw unavailable("对象存储签名失败",ex);}}
    private void ensureBucket(){try{s3.headBucket(HeadBucketRequest.builder().bucket(bucket()).build());}catch(NoSuchBucketException ex){s3.createBucket(CreateBucketRequest.builder().bucket(bucket()).build());}catch(S3Exception ex){if(ex.statusCode()==404)s3.createBucket(CreateBucketRequest.builder().bucket(bucket()).build());else throw ex;}}
    private BusinessException unavailable(String message,RuntimeException cause){return new BusinessException("OBJECT_STORAGE_UNAVAILABLE",message,HttpStatus.SERVICE_UNAVAILABLE);}
}
