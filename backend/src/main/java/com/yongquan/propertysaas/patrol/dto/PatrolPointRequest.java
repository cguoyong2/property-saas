package com.yongquan.propertysaas.patrol.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PatrolPointRequest(
        @NotNull Long projectId,
        @NotBlank @Size(max = 64) String pointCode,
        @NotBlank @Size(max = 100) String pointName,
        @NotBlank String pointType,
        Long equipmentId,
        String location,
        String qrCode,
        String nfcCode,
        String status
) {
}
