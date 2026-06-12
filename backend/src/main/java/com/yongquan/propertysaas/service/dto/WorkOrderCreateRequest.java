package com.yongquan.propertysaas.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record WorkOrderCreateRequest(
        @NotNull Long projectId,
        Long memberId,
        Long houseId,
        @NotBlank String orderType,
        @NotBlank @Size(max = 200) String title,
        String description,
        String location,
        String imageFileIds,
        String priority
) {
}
