package com.yongquan.propertysaas.payment.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PayOrderView(
        Long orderId,
        Long tenantId,
        Long projectId,
        String orderNo,
        Long memberId,
        String payChannel,
        BigDecimal amount,
        String subject,
        String status,
        LocalDateTime expireAt,
        LocalDateTime paidAt,
        String thirdTradeNo,
        LocalDateTime createdAt,
        String memberName,
        String memberMobile,
        Long billCount,
        BigDecimal billAppliedAmount,
        BigDecimal transactionAmount,
        BigDecimal prepaymentAmount,
        BigDecimal prepaymentRemainingAmount
) {
    public PayOrderView(Long orderId, Long tenantId, Long projectId, String orderNo, Long memberId, String payChannel,
                        BigDecimal amount, String subject, String status, LocalDateTime expireAt,
                        LocalDateTime paidAt, String thirdTradeNo, LocalDateTime createdAt) {
        this(orderId, tenantId, projectId, orderNo, memberId, payChannel, amount, subject, status, expireAt,
                paidAt, thirdTradeNo, createdAt, null, null, 0L, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
