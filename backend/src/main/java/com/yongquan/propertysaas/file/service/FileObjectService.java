package com.yongquan.propertysaas.file.service;

import com.yongquan.propertysaas.common.api.PageResult;
import com.yongquan.propertysaas.file.config.FileStorageProperties;
import com.yongquan.propertysaas.file.domain.FileObjectView;
import com.yongquan.propertysaas.file.domain.FileUploadResult;
import com.yongquan.propertysaas.file.domain.StoredFileResource;
import com.yongquan.propertysaas.file.repository.FileObjectRepository;
import com.yongquan.propertysaas.system.audit.domain.OperationLogWrite;
import com.yongquan.propertysaas.system.audit.service.OperationLogService;
import com.yongquan.propertysaas.tenant.context.TenantContext;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileObjectService {

    private static final Set<String> IMAGE_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final FileObjectRepository repository;
    private final FileStorageService storageService;
    private final TenantObjectKeyBuilder objectKeyBuilder;
    private final FileStorageProperties properties;
    private final OperationLogService operationLogService;

    public FileObjectService(FileObjectRepository repository, FileStorageService storageService,
                             TenantObjectKeyBuilder objectKeyBuilder, FileStorageProperties properties,
                             OperationLogService operationLogService) {
        this.repository = repository;
        this.storageService = storageService;
        this.objectKeyBuilder = objectKeyBuilder;
        this.properties = properties;
        this.operationLogService = operationLogService;
    }

    @Transactional
    public FileUploadResult upload(Long projectId, String moduleCode, Boolean sensitive, MultipartFile file, boolean appEndpoint) {
        validateUpload(projectId, moduleCode, file);
        ensureProjectAllowed(projectId);
        Long fileId = newId();
        String originalName = cleanName(file.getOriginalFilename());
        String ext = extension(originalName);
        String normalizedModule = normalizeModule(moduleCode);
        String objectKey = objectKeyBuilder.build(tenantId(), projectId, normalizedModule, fileId, ext, java.time.LocalDate.now());
        try {
            storageService.store(objectKey, file.getInputStream());
        } catch (IOException ex) {
            throw new IllegalArgumentException("文件保存失败", ex);
        }
        FileObjectView view = new FileObjectView(fileId, tenantId(), projectId, normalizedModule, originalName, objectKey,
                ext, file.getContentType(), file.getSize(), Boolean.TRUE.equals(sensitive), uploaderType(),
                userId(), LocalDateTime.now());
        repository.insert(view);
        operationLogService.record(new OperationLogWrite(tenantId(), projectId, "file", "FILE_UPLOAD",
                "file_object", fileId, Map.of(), Map.of("moduleCode", normalizedModule, "fileSize", file.getSize()),
                appEndpoint ? "APP_UPLOAD" : "ADMIN_UPLOAD"));
        return new FileUploadResult(fileId, originalName, normalizedModule, projectId, file.getContentType(), file.getSize(),
                (appEndpoint ? "/api/app/files/" : "/api/files/") + fileId + "/content");
    }

    public PageResult<FileObjectView> page(Long projectId, String moduleCode, long pageNo, long pageSize) {
        validatePage(pageNo, pageSize);
        if (projectId != null) {
            ensureProjectAllowed(projectId);
        }
        Long tenantId = tenantId();
        List<Long> scope = projectScope(tenantId);
        String normalizedModule = moduleCode == null || moduleCode.isBlank() ? null : normalizeModule(moduleCode);
        return new PageResult<>(
                repository.find(tenantId, scope, projectId, normalizedModule, offset(pageNo, pageSize), pageSize),
                repository.count(tenantId, scope, projectId, normalizedModule),
                pageNo,
                pageSize);
    }

    public FileObjectView get(Long fileId) {
        FileObjectView file = repository.get(tenantId(), fileId);
        ensureProjectAllowed(file.projectId());
        return file;
    }

    public StoredFileResource download(Long fileId, boolean ownerOnly) {
        FileObjectView file = get(fileId);
        if (ownerOnly && (!uploaderType().equals(file.uploaderType()) || !userId().equals(file.uploaderId()))) {
            throw new AccessDeniedException("只能下载本人上传的文件");
        }
        return new StoredFileResource(file, storageService.load(file.objectKey()));
    }

    public StoredFileResource loadForBusiness(Long fileId, Long projectId, String moduleCode) {
        FileObjectView file = get(fileId);
        if (!file.projectId().equals(projectId) || !file.moduleCode().equals(normalizeModule(moduleCode))) {
            throw new IllegalArgumentException("源文件不属于当前项目或模块");
        }
        return new StoredFileResource(file, storageService.load(file.objectKey()));
    }

    @Transactional
    public void delete(Long fileId) {
        FileObjectView file = get(fileId);
        int updated = repository.delete(tenantId(), fileId);
        if (updated > 0) {
            storageService.delete(file.objectKey());
            operationLogService.record(new OperationLogWrite(tenantId(), file.projectId(), "file", "FILE_DELETE",
                    "file_object", fileId, Map.of("objectKey", file.objectKey()), Map.of(), null));
        }
    }

    public void validateImageFileIds(Long projectId, String moduleCode, String imageFileIds) {
        List<Long> fileIds = parseFileIds(imageFileIds);
        if (fileIds.isEmpty()) {
            return;
        }
        String normalizedModule = normalizeModule(moduleCode);
        long count = repository.countValidFiles(tenantId(), projectId, normalizedModule, fileIds);
        if (count != fileIds.size()) {
            throw new IllegalArgumentException("图片文件不存在、已删除或不属于当前项目/模块");
        }
    }

    private void validateUpload(Long projectId, String moduleCode, MultipartFile file) {
        if (projectId == null) {
            throw new IllegalArgumentException("projectId 不能为空");
        }
        normalizeModule(moduleCode);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        if (file.getSize() > properties.getMaxFileSizeBytes()) {
            throw new IllegalArgumentException("文件大小超过限制");
        }
        String contentType = file.getContentType();
        if (contentType != null && contentType.startsWith("image/") && !IMAGE_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("不支持的图片类型：" + contentType);
        }
    }

    private List<Long> parseFileIds(String imageFileIds) {
        List<Long> fileIds = new ArrayList<>();
        if (imageFileIds == null || imageFileIds.isBlank()) {
            return fileIds;
        }
        for (String part : imageFileIds.split(",")) {
            String value = part.trim();
            if (!value.isBlank()) {
                fileIds.add(Long.valueOf(value));
            }
        }
        return fileIds;
    }

    private void ensureProjectAllowed(Long projectId) {
        if (!repository.projectExists(tenantId(), projectId)) {
            throw new IllegalArgumentException("项目不存在：" + projectId);
        }
        List<Long> scope = projectScope(tenantId());
        if (scope != null && !scope.contains(projectId)) {
            throw new AccessDeniedException("无项目数据权限：" + projectId);
        }
    }

    private List<Long> projectScope(Long tenantId) {
        if ("MEMBER".equals(TenantContext.getUserType())) {
            return null;
        }
        return repository.findAllowedProjectIds(tenantId, userId());
    }

    private void validatePage(long pageNo, long pageSize) {
        if (pageNo < 1 || pageSize < 1 || pageSize > 200) {
            throw new IllegalArgumentException("分页参数错误");
        }
    }

    private String normalizeModule(String moduleCode) {
        if (moduleCode == null || moduleCode.isBlank()) {
            throw new IllegalArgumentException("moduleCode 不能为空");
        }
        String value = moduleCode.trim().toLowerCase();
        if (!value.matches("[a-z0-9_-]{2,64}")) {
            throw new IllegalArgumentException("非法 moduleCode：" + moduleCode);
        }
        return value;
    }

    private String cleanName(String name) {
        if (name == null || name.isBlank()) {
            return "upload.bin";
        }
        String cleaned = name.replace('\\', '/');
        cleaned = cleaned.substring(cleaned.lastIndexOf('/') + 1).trim();
        return cleaned.isBlank() ? "upload.bin" : cleaned;
    }

    private String extension(String name) {
        int index = name.lastIndexOf('.');
        if (index < 0 || index == name.length() - 1) {
            return "bin";
        }
        return name.substring(index + 1).toLowerCase();
    }

    private String uploaderType() {
        return "MEMBER".equals(TenantContext.getUserType()) ? "MEMBER" : "USER";
    }

    private Long tenantId() {
        return TenantContext.requiredTenantId();
    }

    private Long userId() {
        return TenantContext.getUserId();
    }

    private long offset(long pageNo, long pageSize) {
        return (pageNo - 1) * pageSize;
    }

    private Long newId() {
        return System.currentTimeMillis() * 1000 + ThreadLocalRandom.current().nextInt(1000);
    }
}
