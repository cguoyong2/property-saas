package com.yongquan.propertysaas.patrol.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record PatrolTaskSubmitRequest(
        @NotEmpty List<PatrolTaskSubmitItem> items
) {
}
