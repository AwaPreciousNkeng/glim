package com.codewithpcodes.glimserver.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "glim.storage")
public class StorageProperties {
    private String provider;
    private String endpoint;
    private String region;
    private String accessKey;
    private String secretKey;
    private String bucket;
    private String publicBaseUrl;
    private boolean pathStyleAccess = true;
    private Duration presignUploadTtl = Duration.ofMinutes(15);
    private Duration presignDownloadTtl = Duration.ofHours(1);
}
