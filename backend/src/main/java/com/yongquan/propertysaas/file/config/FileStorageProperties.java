package com.yongquan.propertysaas.file.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "property-saas.file")
public class FileStorageProperties {

    private String objectKeyPattern = "/tenant/{tenantId}/project/{projectId}/module/{module}/yyyy/mm/{fileId}.{ext}";
    private String localRootDir = "data/uploads";
    private long maxFileSizeBytes = 10 * 1024 * 1024;

    public String getObjectKeyPattern() {
        return objectKeyPattern;
    }

    public void setObjectKeyPattern(String objectKeyPattern) {
        this.objectKeyPattern = objectKeyPattern;
    }

    public String getLocalRootDir() {
        return localRootDir;
    }

    public void setLocalRootDir(String localRootDir) {
        this.localRootDir = localRootDir;
    }

    public long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    public void setMaxFileSizeBytes(long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }
}
