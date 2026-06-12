package com.yongquan.propertysaas.file.service;

import java.io.IOException;
import java.io.InputStream;
import org.springframework.core.io.Resource;

public interface FileStorageService {

    void store(String objectKey, InputStream inputStream) throws IOException;

    Resource load(String objectKey);

    void delete(String objectKey);
}
