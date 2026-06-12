package com.yongquan.propertysaas.file.service;

import com.yongquan.propertysaas.file.config.FileStorageProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class LocalFileStorageService implements FileStorageService {

    private final Path rootDir;

    public LocalFileStorageService(FileStorageProperties properties) {
        this.rootDir = Path.of(properties.getLocalRootDir()).toAbsolutePath().normalize();
    }

    @Override
    public void store(String objectKey, InputStream inputStream) throws IOException {
        Path target = resolve(objectKey);
        Files.createDirectories(target.getParent());
        Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public Resource load(String objectKey) {
        Path target = resolve(objectKey);
        if (!Files.exists(target) || !Files.isRegularFile(target)) {
            throw new IllegalArgumentException("文件内容不存在");
        }
        return new FileSystemResource(target);
    }

    @Override
    public void delete(String objectKey) {
        try {
            Files.deleteIfExists(resolve(objectKey));
        } catch (IOException ex) {
            throw new IllegalArgumentException("文件内容删除失败", ex);
        }
    }

    private Path resolve(String objectKey) {
        String normalizedKey = objectKey == null ? "" : objectKey.replace('\\', '/');
        while (normalizedKey.startsWith("/")) {
            normalizedKey = normalizedKey.substring(1);
        }
        Path target = rootDir.resolve(normalizedKey).normalize();
        if (!target.startsWith(rootDir)) {
            throw new IllegalArgumentException("非法文件路径");
        }
        return target;
    }
}
