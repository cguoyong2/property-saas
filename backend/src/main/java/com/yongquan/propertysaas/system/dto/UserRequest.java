package com.yongquan.propertysaas.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record UserRequest(
        Long deptId,
        @NotBlank String username,
        @NotBlank String realName,
        String mobile,
        String password,
        String status,
        @NotNull List<Long> roleIds,
        List<Long> projectIds
) {
}
