package com.yongquan.propertysaas.service.service;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AppMessageService {

    private final JdbcTemplate jdbcTemplate;
    private final AtomicLong idSequence = new AtomicLong(System.currentTimeMillis() * 1000);

    public AppMessageService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record RenderedMessage(String title, String content) {
    }

    public void sendToMember(Long tenantId, Long projectId, Long memberId, String templateCode,
                             String title, String content) {
        if (tenantId == null || memberId == null) {
            return;
        }
        insertMemberMessage(tenantId, projectId, memberId, templateCode, title, content);
    }

    public void sendToMember(Long tenantId, Long projectId, Long memberId, String templateCode,
                             String fallbackTitle, String fallbackContent, Map<String, ?> variables) {
        if (tenantId == null || memberId == null) {
            return;
        }
        RenderedMessage rendered = renderMessage(tenantId, "SITE", templateCode, fallbackTitle, fallbackContent, variables);
        insertMemberMessage(tenantId, projectId, memberId, templateCode, rendered.title(), rendered.content());
    }

    public RenderedMessage renderMessage(Long tenantId, String channel, String templateCode,
                                         String fallbackTitle, String fallbackContent, Map<String, ?> variables) {
        if (tenantId == null || templateCode == null || templateCode.isBlank()) {
            return new RenderedMessage(fallbackTitle, fallbackContent);
        }
        try {
            RenderedMessage template = jdbcTemplate.queryForObject("""
                            SELECT title_template, content_template
                            FROM message_template
                            WHERE tenant_id = ? AND template_code = ? AND channel = ?
                              AND status = 'ACTIVE' AND deleted = 0
                            LIMIT 1
                            """,
                    (rs, rowNum) -> new RenderedMessage(rs.getString("title_template"), rs.getString("content_template")),
                    tenantId, templateCode, channel == null || channel.isBlank() ? "SITE" : channel);
            if (template == null) {
                return new RenderedMessage(fallbackTitle, fallbackContent);
            }
            String title = render(text(template.title(), fallbackTitle), variables);
            String content = render(text(template.content(), fallbackContent), variables);
            return new RenderedMessage(title, content);
        } catch (EmptyResultDataAccessException ex) {
            return new RenderedMessage(fallbackTitle, fallbackContent);
        }
    }

    private void insertMemberMessage(Long tenantId, Long projectId, Long memberId, String templateCode,
                                     String title, String content) {
        jdbcTemplate.update("""
                        INSERT INTO message_record(message_id, tenant_id, project_id, receiver_type, receiver_id,
                                                   receiver_mobile, channel, template_code, title, content, send_status)
                        SELECT ?, ?, ?, 'MEMBER', m.member_id, m.mobile, 'SITE', ?, ?, ?, 'PENDING'
                        FROM member_user m
                        WHERE m.tenant_id = ? AND m.member_id = ? AND m.deleted = 0
                        LIMIT 1
                        """,
                newId(), tenantId, projectId, templateCode, title, content, tenantId, memberId);
    }

    private String render(String template, Map<String, ?> variables) {
        if (template == null || variables == null || variables.isEmpty()) {
            return template;
        }
        String value = template;
        for (Map.Entry<String, ?> entry : variables.entrySet()) {
            String key = entry.getKey();
            String replacement = entry.getValue() == null ? "" : String.valueOf(entry.getValue());
            value = value.replace("${" + key + "}", replacement)
                    .replace("{{" + key + "}}", replacement)
                    .replace("{" + key + "}", replacement);
        }
        return value;
    }

    private String text(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private Long newId() {
        return idSequence.incrementAndGet();
    }
}
