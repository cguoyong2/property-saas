package com.yongquan.propertysaas.base.dto;

import jakarta.validation.constraints.NotBlank;

public record ProjectRequest(
        String projectCode,
        @NotBlank String projectName,
        String projectType,
        String province,
        String city,
        String district,
        String address,
        Long managerUserId,
        String servicePhone,
        String collectionSubject,
        String status
) {
}
