package com.wqst.api.file;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
@Component("objectStorage")
public class ObjectStorageHealthIndicator implements HealthIndicator {
    private final ObjectStorageService storage;
    public ObjectStorageHealthIndicator(ObjectStorageService storage){this.storage=storage;}
    @Override public Health health(){try{storage.healthCheck();return Health.up().withDetail("bucket","private").build();}catch(RuntimeException ex){return Health.down().withDetail("reason","object storage unavailable").build();}}
}
