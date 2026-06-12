package com.yongquan.propertysaas.system.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record RoleMenuRequest(
        @NotNull List<Long> menuIds
) {
}
