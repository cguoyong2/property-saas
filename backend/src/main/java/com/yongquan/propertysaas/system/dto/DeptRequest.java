package com.yongquan.propertysaas.system.dto;

import jakarta.validation.constraints.NotBlank;

public record DeptRequest(
        Long parentId,
        @NotBlank String deptName,
        String deptType,
        Long projectId,
        Integer sortNo,
        String status
) {
}
