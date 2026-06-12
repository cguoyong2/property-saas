package com.yongquan.propertysaas.file.controller;

import com.yongquan.propertysaas.common.api.ApiResponse;
import com.yongquan.propertysaas.common.api.PageResult;
import com.yongquan.propertysaas.file.domain.FileObjectView;
import com.yongquan.propertysaas.file.domain.FileUploadResult;
import com.yongquan.propertysaas.file.domain.StoredFileResource;
import com.yongquan.propertysaas.file.service.FileObjectService;
import com.yongquan.propertysaas.security.permission.RequiresPermission;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class FileObjectController {

    private final FileObjectService service;

    public FileObjectController(FileObjectService service) {
        this.service = service;
    }

    @PostMapping("/api/files")
    @RequiresPermission("file:object:upload")
    public ApiResponse<FileUploadResult> upload(@RequestParam("projectId") Long projectId,
                                                @RequestParam("moduleCode") String moduleCode,
                                                @RequestParam(name = "sensitive", required = false) Boolean sensitive,
                                                @RequestParam("file") MultipartFile file) {
        return ApiResponse.success(service.upload(projectId, moduleCode, sensitive, file, false));
    }

    @GetMapping("/api/files")
    @RequiresPermission("file:object:list")
    public ApiResponse<PageResult<FileObjectView>> page(@RequestParam(name = "projectId", required = false) Long projectId,
                                                        @RequestParam(name = "moduleCode", required = false) String moduleCode,
                                                        @RequestParam(name = "pageNo", defaultValue = "1") long pageNo,
                                                        @RequestParam(name = "pageSize", defaultValue = "20") long pageSize) {
        return ApiResponse.success(service.page(projectId, moduleCode, pageNo, pageSize));
    }

    @GetMapping("/api/files/{fileId}")
    @RequiresPermission("file:object:view")
    public ApiResponse<FileObjectView> get(@PathVariable Long fileId) {
        return ApiResponse.success(service.get(fileId));
    }

    @GetMapping("/api/files/{fileId}/content")
    @RequiresPermission("file:object:download")
    public ResponseEntity<Resource> download(@PathVariable Long fileId) {
        return content(service.download(fileId, false));
    }

    @DeleteMapping("/api/files/{fileId}")
    @RequiresPermission("file:object:delete")
    public ApiResponse<Void> delete(@PathVariable Long fileId) {
        service.delete(fileId);
        return ApiResponse.success();
    }

    @PostMapping("/api/app/files")
    @RequiresPermission("app:file:upload")
    public ApiResponse<FileUploadResult> appUpload(@RequestParam("projectId") Long projectId,
                                                   @RequestParam("moduleCode") String moduleCode,
                                                   @RequestParam(name = "sensitive", required = false) Boolean sensitive,
                                                   @RequestParam("file") MultipartFile file) {
        return ApiResponse.success(service.upload(projectId, moduleCode, sensitive, file, true));
    }

    @GetMapping("/api/app/files/{fileId}/content")
    @RequiresPermission("app:file:download")
    public ResponseEntity<Resource> appDownload(@PathVariable Long fileId) {
        return content(service.download(fileId, true));
    }

    private ResponseEntity<Resource> content(StoredFileResource stored) {
        FileObjectView metadata = stored.metadata();
        String contentType = metadata.contentType() == null || metadata.contentType().isBlank()
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : metadata.contentType();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(metadata.originalName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(metadata.fileSize())
                .body(stored.resource());
    }
}
