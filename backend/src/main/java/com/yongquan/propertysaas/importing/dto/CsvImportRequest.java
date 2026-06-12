package com.yongquan.propertysaas.importing.dto;

import jakarta.validation.constraints.NotNull;

public record CsvImportRequest(
        @NotNull Long projectId,
        @NotNull Long sourceFileId
) {
}
