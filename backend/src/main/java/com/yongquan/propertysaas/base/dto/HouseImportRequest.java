package com.yongquan.propertysaas.base.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record HouseImportRequest(
        @NotNull Long projectId,
        Long sourceFileId,
        @NotNull List<@Valid HouseRequest> rows
) {
}
