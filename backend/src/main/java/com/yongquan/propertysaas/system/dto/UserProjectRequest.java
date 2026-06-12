package com.yongquan.propertysaas.system.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record UserProjectRequest(
        @NotNull List<Long> projectIds
) {
}
