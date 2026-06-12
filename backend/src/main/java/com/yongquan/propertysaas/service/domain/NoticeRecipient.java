package com.yongquan.propertysaas.service.domain;

public record NoticeRecipient(
        String receiverType,
        Long receiverId,
        String receiverMobile
) {
}
