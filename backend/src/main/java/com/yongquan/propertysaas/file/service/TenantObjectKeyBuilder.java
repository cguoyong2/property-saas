package com.yongquan.propertysaas.file.service;

import com.yongquan.propertysaas.file.config.FileStorageProperties;
import com.yongquan.propertysaas.tenant.context.TenantContext;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

@Component
public class TenantObjectKeyBuilder {

    private static final DateTimeFormatter YEAR_FORMAT = DateTimeFormatter.ofPattern("yyyy");
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("MM");

    private final FileStorageProperties fileStorageProperties;

    public TenantObjectKeyBuilder(FileStorageProperties fileStorageProperties) {
        this.fileStorageProperties = fileStorageProperties;
    }

    public String build(Long projectId, String module, Long fileId, String ext) {
        return build(TenantContext.requiredTenantId(), projectId, module, fileId, ext, LocalDate.now());
    }

    public String build(Long tenantId, Long projectId, String module, Long fileId, String ext, LocalDate date) {
        if (projectId == null) {
            throw new IllegalArgumentException("project_id is required for tenant business files");
        }
        if (module == null || module.isBlank()) {
            throw new IllegalArgumentException("module is required");
        }
        if (fileId == null) {
            throw new IllegalArgumentException("file_id is required");
        }
        String normalizedExt = ext == null || ext.isBlank() ? "bin" : ext.replace(".", "");
        return fileStorageProperties.getObjectKeyPattern()
                .replace("{tenantId}", String.valueOf(tenantId))
                .replace("{projectId}", String.valueOf(projectId))
                .replace("{module}", module)
                .replace("yyyy", YEAR_FORMAT.format(date))
                .replace("mm", MONTH_FORMAT.format(date))
                .replace("{fileId}", String.valueOf(fileId))
                .replace("{ext}", normalizedExt);
    }
}
