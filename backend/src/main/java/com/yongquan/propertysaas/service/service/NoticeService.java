package com.yongquan.propertysaas.service.service;

import com.yongquan.propertysaas.common.api.PageResult;
import com.yongquan.propertysaas.service.domain.MessageDispatchResult;
import com.yongquan.propertysaas.service.domain.MessageRecordView;
import com.yongquan.propertysaas.service.domain.MessageTemplateView;
import com.yongquan.propertysaas.service.domain.NoticeRecipient;
import com.yongquan.propertysaas.service.domain.NoticeView;
import com.yongquan.propertysaas.service.dto.MessageTemplateRequest;
import com.yongquan.propertysaas.service.dto.NoticeCreateRequest;
import com.yongquan.propertysaas.service.dto.PlatformNoticeCreateRequest;
import com.yongquan.propertysaas.service.repository.NoticeRepository;
import com.yongquan.propertysaas.tenant.context.TenantContext;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NoticeService {

    private static final Set<String> NOTICE_TYPES = Set.of("PROPERTY", "SYSTEM", "PAYMENT", "WORKORDER", "EMERGENCY");
    private static final Set<String> TARGET_SCOPES = Set.of("ALL_TENANT", "PROJECT", "HOUSE", "MEMBER");
    private static final Set<String> NOTICE_STATUSES = Set.of("DRAFT", "PUBLISHED", "WITHDRAWN");
    private static final Set<String> CHANNELS = Set.of("SITE", "SMS", "WECHAT");
    private static final Set<String> MESSAGE_STATUSES = Set.of("PENDING", "SENT", "FAILED");
    private static final Set<String> READ_STATUSES = Set.of("UNREAD", "READ");
    private static final Set<String> TEMPLATE_STATUSES = Set.of("ACTIVE", "DISABLED");

    private final NoticeRepository repository;
    private final AtomicLong idSequence = new AtomicLong(System.currentTimeMillis() * 1000);

    public NoticeService(NoticeRepository repository) {
        this.repository = repository;
    }

    public PageResult<NoticeView> pagePlatformNotices(Long tenantId, Long projectId, String status,
                                                       long pageNo, long pageSize) {
        validatePage(pageNo, pageSize);
        validateNoticeStatus(status);
        if (tenantId != null && !repository.tenantExists(tenantId)) {
            throw new IllegalArgumentException("租户不存在：" + tenantId);
        }
        if (projectId != null) {
            if (tenantId == null) {
                throw new IllegalArgumentException("按项目查询平台公告时必须传 tenantId");
            }
            ensureProjectExists(tenantId, projectId);
        }
        return new PageResult<>(
                repository.findPlatformNotices(tenantId, projectId, status, offset(pageNo, pageSize), pageSize),
                repository.countPlatformNotices(tenantId, projectId, status),
                pageNo,
                pageSize);
    }

    @Transactional
    public Long createPlatformNotice(PlatformNoticeCreateRequest request) {
        if (!repository.tenantExists(request.tenantId())) {
            throw new IllegalArgumentException("租户不存在：" + request.tenantId());
        }
        if (request.projectId() != null) {
            ensureProjectExists(request.tenantId(), request.projectId());
        }
        NoticeCreateRequest tenantRequest = new NoticeCreateRequest(request.projectId(), request.title(),
                request.content(), request.noticeType(), request.targetScope(), null, null,
                request.channels(), request.templateCode(), request.publishNow());
        Long noticeId = createNoticeForTenant(request.tenantId(), tenantRequest, true);
        if (!Boolean.FALSE.equals(request.publishNow())) {
            insertMessages(request.tenantId(), request.projectId(), tenantRequest, repository.findTenantAdminRecipients(request.tenantId()));
        }
        return noticeId;
    }

    public PageResult<NoticeView> pageTenantNotices(Long projectId, String status, long pageNo, long pageSize) {
        validatePage(pageNo, pageSize);
        validateNoticeStatus(status);
        if (projectId != null) {
            ensureProjectAllowed(projectId);
        }
        Long tenantId = tenantId();
        List<Long> scope = projectScope(tenantId);
        return new PageResult<>(
                repository.findTenantNotices(tenantId, scope, projectId, status, offset(pageNo, pageSize), pageSize),
                repository.countTenantNotices(tenantId, scope, projectId, status),
                pageNo,
                pageSize);
    }

    public NoticeView getNotice(Long noticeId) {
        NoticeView notice = repository.getNotice(tenantId(), noticeId);
        if (notice.projectId() != null) {
            ensureProjectAllowed(notice.projectId());
        }
        return notice;
    }

    @Transactional
    public Long createTenantNotice(NoticeCreateRequest request) {
        return createNoticeForTenant(tenantId(), request, false);
    }

    @Transactional
    public void publishNotice(Long noticeId) {
        NoticeView notice = getNotice(noticeId);
        if (repository.publishNotice(tenantId(), noticeId, userId()) == 0) {
            throw new IllegalArgumentException("公告状态已变化，发布失败");
        }
        insertMessages(tenantId(), notice.projectId(), noticeToRequest(notice), recipients(tenantId(), notice.projectId(), noticeToRequest(notice)));
    }

    @Transactional
    public void withdrawNotice(Long noticeId) {
        NoticeView notice = getNotice(noticeId);
        if (notice.projectId() != null) {
            ensureProjectAllowed(notice.projectId());
        }
        if (repository.withdrawNotice(tenantId(), noticeId, userId()) == 0) {
            throw new IllegalArgumentException("公告状态已变化，撤回失败");
        }
    }

    public PageResult<MessageRecordView> pageMessages(Long projectId, String channel, String status,
                                                       String templateCode, String receiverType, String readStatus,
                                                       long pageNo, long pageSize) {
        validatePage(pageNo, pageSize);
        validateChannelIfPresent(channel);
        validateMessageStatus(status);
        validateReadStatus(readStatus);
        if (projectId != null) {
            ensureProjectAllowed(projectId);
        }
        Long tenantId = tenantId();
        List<Long> scope = projectScope(tenantId);
        return new PageResult<>(
                repository.findMessages(tenantId, scope, projectId, normalizeNullable(channel), normalizeNullable(status),
                        normalizeNullable(templateCode), normalizeNullable(receiverType), normalizeNullable(readStatus),
                        offset(pageNo, pageSize), pageSize),
                repository.countMessages(tenantId, scope, projectId, normalizeNullable(channel), normalizeNullable(status),
                        normalizeNullable(templateCode), normalizeNullable(receiverType), normalizeNullable(readStatus)),
                pageNo,
                pageSize);
    }

    public PageResult<MessageTemplateView> pageMessageTemplates(String channel, String status,
                                                                long pageNo, long pageSize) {
        validatePage(pageNo, pageSize);
        validateChannelIfPresent(channel);
        validateTemplateStatus(status);
        Long tenantId = tenantId();
        return new PageResult<>(
                repository.findMessageTemplates(tenantId, normalizeNullable(channel), normalizeNullable(status),
                        offset(pageNo, pageSize), pageSize),
                repository.countMessageTemplates(tenantId, normalizeNullable(channel), normalizeNullable(status)),
                pageNo,
                pageSize);
    }

    @Transactional
    public Long createMessageTemplate(MessageTemplateRequest request) {
        String channel = normalize(request.channel(), "SITE");
        String status = normalize(request.status(), "ACTIVE");
        validateChannelIfPresent(channel);
        validateTemplateStatus(status);
        ensureTemplateCodeUnique(null, request.templateCode(), channel);
        Long templateId = newId();
        repository.insertMessageTemplate(tenantId(), templateId, userId(), request, channel, status);
        return templateId;
    }

    @Transactional
    public void updateMessageTemplate(Long templateId, MessageTemplateRequest request) {
        repository.getMessageTemplate(tenantId(), templateId);
        String channel = normalize(request.channel(), "SITE");
        String status = normalize(request.status(), "ACTIVE");
        validateChannelIfPresent(channel);
        validateTemplateStatus(status);
        ensureTemplateCodeUnique(templateId, request.templateCode(), channel);
        if (repository.updateMessageTemplate(tenantId(), templateId, userId(), request, channel, status) == 0) {
            throw new IllegalArgumentException("消息模板不存在：" + templateId);
        }
    }

    @Transactional
    public MessageDispatchResult dispatchPendingMessages(Integer limit) {
        return dispatchMessages("PENDING", limit);
    }

    @Transactional
    public MessageDispatchResult retryFailedMessages(Integer limit) {
        return dispatchMessages("FAILED", limit);
    }

    @Transactional
    public MessageDispatchResult retryMessage(Long messageId) {
        MessageRecordView message = repository.getMessage(tenantId(), messageId);
        ensureMessageAllowed(message);
        if ("SENT".equals(message.sendStatus())) {
            return new MessageDispatchResult(1, 0, 0, 1);
        }
        return dispatchOne(message);
    }

    public PageResult<NoticeView> pageAppNotices(Long projectId, Long memberId, long pageNo, long pageSize) {
        validatePage(pageNo, pageSize);
        if (projectId != null) {
            ensureProjectAllowed(projectId);
        }
        return new PageResult<>(
                repository.findAppNotices(tenantId(), projectId, memberId, offset(pageNo, pageSize), pageSize),
                repository.countAppNotices(tenantId(), projectId, memberId),
                pageNo,
                pageSize);
    }

    public PageResult<MessageRecordView> pageAppMessages(Long projectId, String readStatus, long pageNo, long pageSize) {
        validatePage(pageNo, pageSize);
        validateReadStatus(readStatus);
        if (projectId != null) {
            ensureProjectAllowed(projectId);
        }
        String status = normalizeNullable(readStatus);
        return new PageResult<>(
                repository.findAppMessages(tenantId(), userId(), projectId, status, offset(pageNo, pageSize), pageSize),
                repository.countAppMessages(tenantId(), userId(), projectId, status),
                pageNo,
                pageSize);
    }

    public MessageRecordView getAppMessage(Long messageId) {
        MessageRecordView message = repository.getAppMessage(tenantId(), userId(), messageId);
        if (message.projectId() != null) {
            ensureProjectAllowed(message.projectId());
        }
        return message;
    }

    @Transactional
    public void readAppMessage(Long messageId) {
        MessageRecordView message = getAppMessage(messageId);
        if (!"READ".equals(message.readStatus())) {
            repository.markAppMessageRead(tenantId(), userId(), messageId);
        }
    }

    @Transactional
    public Map<String, Integer> readAllAppMessages(Long projectId) {
        if (projectId != null) {
            ensureProjectAllowed(projectId);
        }
        int affected = repository.markAllAppMessagesRead(tenantId(), userId(), projectId);
        return Map.of("affected", affected);
    }

    public Map<String, Long> appUnreadSummary(Long projectId) {
        if (projectId != null) {
            ensureProjectAllowed(projectId);
        }
        long unread = repository.countAppMessages(tenantId(), userId(), projectId, "UNREAD");
        return Map.of("unreadCount", unread);
    }

    public PageResult<NoticeView> pagePublicNotices(Long tenantId, Long projectId, long pageNo, long pageSize) {
        validatePage(pageNo, pageSize);
        if (!repository.tenantExists(tenantId)) {
            throw new IllegalArgumentException("租户不存在：" + tenantId);
        }
        if (projectId != null) {
            ensureProjectExists(tenantId, projectId);
        }
        return new PageResult<>(
                repository.findPublicNotices(tenantId, projectId, offset(pageNo, pageSize), pageSize),
                repository.countPublicNotices(tenantId, projectId),
                pageNo,
                pageSize);
    }

    private Long createNoticeForTenant(Long tenantId, NoticeCreateRequest request, boolean platformCreate) {
        String noticeType = normalize(request.noticeType(), "PROPERTY");
        String targetScope = normalize(request.targetScope(), request.projectId() == null ? "ALL_TENANT" : "PROJECT");
        validateNoticeType(noticeType);
        validateTargetScope(targetScope);
        validateChannels(request.channels());
        validateTarget(tenantId, request, targetScope, platformCreate);
        Long noticeId = newId();
        String publishStatus = Boolean.FALSE.equals(request.publishNow()) ? "DRAFT" : "PUBLISHED";
        repository.insertNotice(tenantId, noticeId, userId(), request, noticeType, targetScope, publishStatus);
        if ("PUBLISHED".equals(publishStatus)) {
            insertMessages(tenantId, request.projectId(), request, recipients(tenantId, request.projectId(), request));
        }
        return noticeId;
    }

    private void validateTarget(Long tenantId, NoticeCreateRequest request, String targetScope, boolean platformCreate) {
        if (request.projectId() != null) {
            ensureProjectExists(tenantId, request.projectId());
            if (!platformCreate) {
                ensureProjectAllowed(request.projectId());
            }
        }
        if ("PROJECT".equals(targetScope) && request.projectId() == null) {
            throw new IllegalArgumentException("项目公告必须传 projectId");
        }
        if ("HOUSE".equals(targetScope)) {
            if (request.projectId() == null) {
                throw new IllegalArgumentException("房屋定向通知必须传 projectId");
            }
            if (request.targetHouseIds() == null || request.targetHouseIds().isEmpty()) {
                throw new IllegalArgumentException("房屋定向通知必须传 targetHouseIds");
            }
        }
        if ("MEMBER".equals(targetScope) && (request.targetMemberIds() == null || request.targetMemberIds().isEmpty())) {
            throw new IllegalArgumentException("会员定向通知必须传 targetMemberIds");
        }
    }

    private List<NoticeRecipient> recipients(Long tenantId, Long projectId, NoticeCreateRequest request) {
        String targetScope = normalize(request.targetScope(), projectId == null ? "ALL_TENANT" : "PROJECT");
        return switch (targetScope) {
            case "ALL_TENANT" -> repository.findTenantMemberRecipients(tenantId);
            case "PROJECT" -> repository.findProjectMemberRecipients(tenantId, projectId);
            case "MEMBER" -> repository.findMemberRecipients(tenantId, projectId, request.targetMemberIds());
            case "HOUSE" -> repository.findHouseMemberRecipients(tenantId, projectId, request.targetHouseIds());
            default -> List.of();
        };
    }

    private void insertMessages(Long tenantId, Long projectId, NoticeCreateRequest request, List<NoticeRecipient> recipients) {
        if (recipients.isEmpty()) {
            return;
        }
        for (String channel : channels(request.channels())) {
            for (NoticeRecipient recipient : deduplicate(recipients)) {
                repository.insertMessage(newId(), tenantId, projectId, recipient, channel, request.templateCode(),
                        request.title(), request.content());
            }
        }
    }

    private List<NoticeRecipient> deduplicate(List<NoticeRecipient> recipients) {
        Map<String, NoticeRecipient> unique = new LinkedHashMap<>();
        for (NoticeRecipient recipient : recipients) {
            unique.put(recipient.receiverType() + ":" + recipient.receiverId() + ":" + recipient.receiverMobile(), recipient);
        }
        return List.copyOf(unique.values());
    }

    private NoticeCreateRequest noticeToRequest(NoticeView notice) {
        return new NoticeCreateRequest(notice.projectId(), notice.title(), notice.content(), notice.noticeType(),
                notice.targetScope(), null, null, List.of("SITE"), null, true);
    }

    private void ensureProjectAllowed(Long projectId) {
        ensureProjectExists(tenantId(), projectId);
        List<Long> scope = projectScope(tenantId());
        if (scope != null && !scope.contains(projectId)) {
            throw new AccessDeniedException("无项目数据权限：" + projectId);
        }
    }

    private void ensureProjectExists(Long tenantId, Long projectId) {
        if (!repository.projectExists(tenantId, projectId)) {
            throw new IllegalArgumentException("项目不存在：" + projectId);
        }
    }

    private List<Long> projectScope(Long tenantId) {
        return repository.findAllowedProjectIds(tenantId, userId());
    }

    private void validatePage(long pageNo, long pageSize) {
        if (pageNo < 1 || pageSize < 1 || pageSize > 200) {
            throw new IllegalArgumentException("分页参数错误");
        }
    }

    private void validateNoticeType(String noticeType) {
        if (!NOTICE_TYPES.contains(noticeType)) {
            throw new IllegalArgumentException("非法公告类型：" + noticeType);
        }
    }

    private void validateTargetScope(String targetScope) {
        if (!TARGET_SCOPES.contains(targetScope)) {
            throw new IllegalArgumentException("非法通知范围：" + targetScope);
        }
    }

    private void validateNoticeStatus(String status) {
        if (status != null && !status.isBlank() && !NOTICE_STATUSES.contains(status)) {
            throw new IllegalArgumentException("非法公告状态：" + status);
        }
    }

    private void validateChannelIfPresent(String channel) {
        if (channel != null && !channel.isBlank() && !CHANNELS.contains(channel)) {
            throw new IllegalArgumentException("非法消息渠道：" + channel);
        }
    }

    private void validateMessageStatus(String status) {
        if (status != null && !status.isBlank() && !MESSAGE_STATUSES.contains(status)) {
            throw new IllegalArgumentException("非法消息状态：" + status);
        }
    }

    private void validateReadStatus(String status) {
        if (status != null && !status.isBlank() && !READ_STATUSES.contains(status.toUpperCase())) {
            throw new IllegalArgumentException("非法已读状态：" + status);
        }
    }

    private void validateTemplateStatus(String status) {
        if (status != null && !status.isBlank() && !TEMPLATE_STATUSES.contains(status)) {
            throw new IllegalArgumentException("非法模板状态：" + status);
        }
    }

    private void validateChannels(List<String> channels) {
        for (String channel : channels(channels)) {
            if (!CHANNELS.contains(channel)) {
                throw new IllegalArgumentException("非法消息渠道：" + channel);
            }
        }
    }

    private List<String> channels(List<String> channels) {
        if (channels == null || channels.isEmpty()) {
            return List.of("SITE");
        }
        return channels.stream().map(channel -> normalize(channel, "SITE")).distinct().toList();
    }

    private String normalize(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim().toUpperCase();
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase();
    }

    private void ensureTemplateCodeUnique(Long templateId, String templateCode, String channel) {
        if (repository.messageTemplateCodeExists(tenantId(), templateId, templateCode, channel)) {
            throw new IllegalArgumentException("同渠道模板编码已存在：" + templateCode);
        }
    }

    private MessageDispatchResult dispatchMessages(String status, Integer limit) {
        int effectiveLimit = Math.max(1, Math.min(limit == null ? 200 : limit, 1000));
        List<MessageRecordView> messages = repository.findDispatchCandidates(tenantId(), projectScope(tenantId()),
                status, effectiveLimit);
        int total = 0;
        int sent = 0;
        int failed = 0;
        int skipped = 0;
        for (MessageRecordView message : messages) {
            MessageDispatchResult result = dispatchOne(message);
            total += result.totalCount();
            sent += result.sentCount();
            failed += result.failedCount();
            skipped += result.skippedCount();
        }
        return new MessageDispatchResult(total, sent, failed, skipped);
    }

    private MessageDispatchResult dispatchOne(MessageRecordView message) {
        if (!"PENDING".equals(message.sendStatus()) && !"FAILED".equals(message.sendStatus())) {
            return new MessageDispatchResult(1, 0, 0, 1);
        }
        if ("SITE".equals(message.channel())) {
            int updated = repository.markMessageSent(tenantId(), message.messageId());
            return updated == 0
                    ? new MessageDispatchResult(1, 0, 0, 1)
                    : new MessageDispatchResult(1, 1, 0, 0);
        }
        String reason = "真实" + message.channel() + "通道未配置，等待正式部署或厂商资料齐备后接入";
        int updated = repository.markMessageFailed(tenantId(), message.messageId(), reason);
        return updated == 0
                ? new MessageDispatchResult(1, 0, 0, 1)
                : new MessageDispatchResult(1, 0, 1, 0);
    }

    private void ensureMessageAllowed(MessageRecordView message) {
        if (message.projectId() != null) {
            ensureProjectAllowed(message.projectId());
        }
    }

    private Long tenantId() {
        return TenantContext.requiredTenantId();
    }

    private Long userId() {
        return TenantContext.getUserId();
    }

    private long offset(long pageNo, long pageSize) {
        return (pageNo - 1) * pageSize;
    }

    private Long newId() {
        return idSequence.incrementAndGet();
    }
}
