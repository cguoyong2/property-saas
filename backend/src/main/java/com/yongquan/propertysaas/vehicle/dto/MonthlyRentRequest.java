package com.yongquan.propertysaas.vehicle.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record MonthlyRentRequest(
        @NotBlank String monthlyRentStatus,
        LocalDate startDate,
        LocalDate endDate
) {
}
