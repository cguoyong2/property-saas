package com.yongquan.propertysaas.service.service;

import java.util.concurrent.atomic.AtomicLong;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AppMessageService {

    private final JdbcTemplate jdbcTemplate;
    private final AtomicLong idSequence = new AtomicLong(System.currentTimeMillis() * 1000);

    public AppMessageService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void sendToMember(Long tenantId, Long projectId, Long memberId, String templateCode,
                             String title, String content) {
        if (tenantId == null || memberId == null) {
            return;
        }
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

    private Long newId() {
        return idSequence.incrementAndGet();
    }
}
