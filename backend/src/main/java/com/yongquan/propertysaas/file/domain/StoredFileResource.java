package com.yongquan.propertysaas.file.domain;

import org.springframework.core.io.Resource;

public record StoredFileResource(
        FileObjectView metadata,
        Resource resource
) {
}
