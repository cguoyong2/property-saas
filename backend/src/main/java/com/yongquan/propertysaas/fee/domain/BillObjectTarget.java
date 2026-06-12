package com.yongquan.propertysaas.fee.domain;

public record BillObjectTarget(
        Long memberId,
        Long houseId,
        String mobile
) {
}
