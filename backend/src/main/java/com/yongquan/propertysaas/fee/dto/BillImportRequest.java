package com.yongquan.propertysaas.fee.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record BillImportRequest(
        @NotNull Long projectId,
        Long sourceFileId,
        @NotEmpty List<@Valid BillManualRequest> rows
) {
}
