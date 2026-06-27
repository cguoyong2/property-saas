package com.yongquan.propertysaas.service.controller;

import com.yongquan.propertysaas.common.api.ApiResponse;
import com.yongquan.propertysaas.common.api.PageResult;
import com.yongquan.propertysaas.security.permission.RequiresPermission;
import com.yongquan.propertysaas.service.domain.MessageDispatchResult;
import com.yongquan.propertysaas.service.domain.MessageRecordView;
import com.yongquan.propertysaas.service.domain.MessageTemplateView;
import com.yongquan.propertysaas.service.domain.NoticeView;
import com.yongquan.propertysaas.service.dto.MessageTemplateRequest;
import com.yongquan.propertysaas.service.dto.NoticeCreateRequest;
import com.yongquan.propertysaas.service.dto.PlatformNoticeCreateRequest;
import com.yongquan.propertysaas.service.service.NoticeService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NoticeController {

    private final NoticeService service;

    public NoticeController(NoticeService service) {
        this.service = service;
    }

    @GetMapping("/api/platform/notices")
    @RequiresPermission(value = "platform:notice:list", platformOnly = true)
    public ApiResponse<PageResult<NoticeView>> pagePlatformNotices(@RequestParam(required = false) Long tenantId,
                                                                   @RequestParam(required = false) Long projectId,
                                                                   @RequestParam(required = false) String status,
                                                                   @RequestParam(defaultValue = "1") long pageNo,
                                                                   @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(service.pagePlatformNotices(tenantId, projectId, status, pageNo, pageSize));
    }

    @PostMapping("/api/platform/notices")
    @RequiresPermission(value = "platform:notice:create", platformOnly = true)
    public ApiResponse<Map<String, Long>> createPlatformNotice(@Valid @RequestBody PlatformNoticeCreateRequest request) {
        return ApiResponse.success(Map.of("noticeId", service.createPlatformNotice(request)));
    }

    @GetMapping("/api/service/notices")
    @RequiresPermission("service:notice:list")
    public ApiResponse<PageResult<NoticeView>> pageTenantNotices(@RequestParam(required = false) Long projectId,
                                                                 @RequestParam(required = false) String status,
                                                                 @RequestParam(defaultValue = "1") long pageNo,
                                                                 @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(service.pageTenantNotices(projectId, status, pageNo, pageSize));
    }

    @PostMapping("/api/service/notices")
    @RequiresPermission("service:notice:create")
    public ApiResponse<Map<String, Long>> createTenantNotice(@Valid @RequestBody NoticeCreateRequest request) {
        return ApiResponse.success(Map.of("noticeId", service.createTenantNotice(request)));
    }

    @GetMapping("/api/service/notices/{noticeId}")
    @RequiresPermission("service:notice:view")
    public ApiResponse<NoticeView> getNotice(@PathVariable Long noticeId) {
        return ApiResponse.success(service.getNotice(noticeId));
    }

    @PutMapping("/api/service/notices/{noticeId}/publish")
    @RequiresPermission("service:notice:publish")
    public ApiResponse<Void> publishNotice(@PathVariable Long noticeId) {
        service.publishNotice(noticeId);
        return ApiResponse.success();
    }

    @PutMapping("/api/service/notices/{noticeId}/withdraw")
    @RequiresPermission("service:notice:withdraw")
    public ApiResponse<Void> withdrawNotice(@PathVariable Long noticeId) {
        service.withdrawNotice(noticeId);
        return ApiResponse.success();
    }

    @GetMapping("/api/service/messages")
    @RequiresPermission("service:message:list")
    public ApiResponse<PageResult<MessageRecordView>> pageMessages(@RequestParam(required = false) Long projectId,
                                                                   @RequestParam(required = false) String channel,
                                                                   @RequestParam(required = false) String status,
                                                                   @RequestParam(defaultValue = "1") long pageNo,
                                                                   @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(service.pageMessages(projectId, channel, status, pageNo, pageSize));
    }

    @GetMapping("/api/service/message-templates")
    @RequiresPermission("service:messageTemplate:list")
    public ApiResponse<PageResult<MessageTemplateView>> pageMessageTemplates(@RequestParam(required = false) String channel,
                                                                             @RequestParam(required = false) String status,
                                                                             @RequestParam(defaultValue = "1") long pageNo,
                                                                             @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(service.pageMessageTemplates(channel, status, pageNo, pageSize));
    }

    @PostMapping("/api/service/message-templates")
    @RequiresPermission("service:messageTemplate:create")
    public ApiResponse<Map<String, Long>> createMessageTemplate(@Valid @RequestBody MessageTemplateRequest request) {
        return ApiResponse.success(Map.of("templateId", service.createMessageTemplate(request)));
    }

    @PutMapping("/api/service/message-templates/{templateId}")
    @RequiresPermission("service:messageTemplate:update")
    public ApiResponse<Void> updateMessageTemplate(@PathVariable Long templateId,
                                                   @Valid @RequestBody MessageTemplateRequest request) {
        service.updateMessageTemplate(templateId, request);
        return ApiResponse.success();
    }

    @PostMapping("/api/service/messages/dispatch-pending")
    @RequiresPermission("service:message:dispatch")
    public ApiResponse<MessageDispatchResult> dispatchPendingMessages(@RequestParam(required = false) Integer limit) {
        return ApiResponse.success(service.dispatchPendingMessages(limit));
    }

    @PostMapping("/api/service/messages/retry-failed")
    @RequiresPermission("service:message:retry")
    public ApiResponse<MessageDispatchResult> retryFailedMessages(@RequestParam(required = false) Integer limit) {
        return ApiResponse.success(service.retryFailedMessages(limit));
    }

    @PostMapping("/api/service/messages/{messageId}/retry")
    @RequiresPermission("service:message:retry")
    public ApiResponse<MessageDispatchResult> retryMessage(@PathVariable Long messageId) {
        return ApiResponse.success(service.retryMessage(messageId));
    }

    @GetMapping("/api/app/notices")
    @RequiresPermission("app:notice:list")
    public ApiResponse<PageResult<NoticeView>> pageAppNotices(@RequestParam(required = false) Long projectId,
                                                              @RequestParam(required = false) Long memberId,
                                                              @RequestParam(defaultValue = "1") long pageNo,
                                                              @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(service.pageAppNotices(projectId, memberId, pageNo, pageSize));
    }

    @GetMapping("/api/public/notices")
    public ApiResponse<PageResult<NoticeView>> pagePublicNotices(@RequestParam Long tenantId,
                                                                 @RequestParam(required = false) Long projectId,
                                                                 @RequestParam(defaultValue = "1") long pageNo,
                                                                 @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(service.pagePublicNotices(tenantId, projectId, pageNo, pageSize));
    }
}
