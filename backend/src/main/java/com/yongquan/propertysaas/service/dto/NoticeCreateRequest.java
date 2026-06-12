package com.yongquan.propertysaas.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record NoticeCreateRequest(
        Long projectId,
        @NotBlank @Size(max = 200) String title,
        @NotBlank String content,
        String noticeType,
        String targetScope,
        List<Long> targetMemberIds,
        List<Long> targetHouseIds,
        List<String> channels,
        String templateCode,
        Boolean publishNow
) {
}
