package com.yongquan.propertysaas.base.domain;

import java.time.LocalDateTime;

public record ProjectView(
        Long projectId,
        String projectCode,
        String projectName,
        String projectType,
        String province,
        String city,
        String district,
        String address,
        Long managerUserId,
        String servicePhone,
        String collectionSubject,
        String status,
        LocalDateTime createdAt
) {
}
