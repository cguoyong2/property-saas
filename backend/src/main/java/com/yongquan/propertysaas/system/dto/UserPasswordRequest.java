package com.yongquan.propertysaas.system.dto;

import jakarta.validation.constraints.NotBlank;

public record UserPasswordRequest(@NotBlank String password) {
}
