package com.yongquan.propertysaas.platform.repository;

import com.yongquan.propertysaas.platform.domain.DashboardView;
import com.yongquan.propertysaas.platform.domain.MonitorAlertView;
import com.yongquan.propertysaas.platform.domain.PackageView;
import com.yongquan.propertysaas.platform.domain.PlatformMonitorView;
import com.yongquan.propertysaas.platform.domain.TenantConfigView;
import com.yongquan.propertysaas.platform.domain.TenantSummary;
import com.yongquan.propertysaas.platform.domain.UsageView;
import com.yongquan.propertysaas.platform.dto.PackageRequest;
import com.yongquan.propertysaas.platform.dto.TenantConfigRequest;
import com.yongquan.propertysaas.platform.dto.TenantRequest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PlatformTenantRepository {

    private final JdbcTemplate jdbcTemplate;

    public PlatformTenantRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public DashboardView dashboard() {
        Long tenantCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_tenant WHERE deleted = 0", Long.class);
        Long activeTenantCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_tenant WHERE deleted = 0 AND status = 'ACTIVE'", Long.class);
        Long packageCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_tenant_package WHERE deleted = 0", Long.class);
        return new DashboardView(value(tenantCount), value(activeTenantCount), value(packageCount));
    }

    public PlatformMonitorView monitor() {
        long interfaceFailureCount = count("""
                SELECT COUNT(*) FROM interface_call_log
                WHERE success = 0 AND created_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
                """);
        long interfaceRetryPendingCount = count("""
                SELECT COUNT(*) FROM interface_call_log
                WHERE success = 0 AND next_retry_at IS NOT NULL AND next_retry_at <= NOW()
                """);
        long messageFailedCount = count("""
                SELECT COUNT(*) FROM message_record
                WHERE send_status = 'FAILED' AND created_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
                """);
        long messagePendingCount = count("""
                SELECT COUNT(*) FROM message_record
                WHERE send_status = 'PENDING'
                """);
        long paymentFailedCount = count("""
                SELECT COUNT(*) FROM pay_order
                WHERE deleted = 0 AND status IN ('FAILED', 'CLOSED') AND created_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
                """);
        long refundFailedCount = count("""
                SELECT COUNT(*) FROM pay_refund
                WHERE deleted = 0 AND status IN ('FAILED', 'AUDIT_REJECTED') AND created_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
                """);
        long loginFailedCount = count("""
                SELECT COUNT(*) FROM sys_login_log
                WHERE login_result = 'FAILED' AND created_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
                """);
        long highRiskOperationCount = count("""
                SELECT COUNT(*) FROM operation_log
                WHERE created_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
                """);
        List<MonitorAlertView> alerts = new ArrayList<>();
        addAlert(alerts, "INTERFACE_FAILURE", interfaceFailureCount, "HIGH", "近 24 小时存在第三方接口调用失败");
        addAlert(alerts, "INTERFACE_RETRY_PENDING", interfaceRetryPendingCount, "MEDIUM", "存在到期未重试的第三方接口调用");
        addAlert(alerts, "MESSAGE_FAILED", messageFailedCount, "MEDIUM", "近 24 小时存在消息发送失败");
        addAlert(alerts, "MESSAGE_PENDING", messagePendingCount, "LOW", "存在待派发消息");
        addAlert(alerts, "PAYMENT_FAILED", paymentFailedCount, "HIGH", "近 24 小时存在支付失败或关闭订单");
        addAlert(alerts, "REFUND_FAILED", refundFailedCount, "HIGH", "近 24 小时存在退款失败或驳回记录");
        addAlert(alerts, "LOGIN_FAILED", loginFailedCount, "MEDIUM", "近 24 小时存在登录失败");
        return new PlatformMonitorView(LocalDateTime.now(), interfaceFailureCount, interfaceRetryPendingCount,
                messageFailedCount, messagePendingCount, paymentFailedCount, refundFailedCount, loginFailedCount,
                highRiskOperationCount, alerts);
    }

    public List<TenantSummary> findTenants(String keyword, String status, long offset, long pageSize) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT tenant_id, tenant_name, tenant_code, contact_name, contact_mobile, package_id,
                       service_start_date, service_end_date, status, created_at
                FROM sys_tenant
                WHERE deleted = 0
                """);
        appendTenantFilters(sql, args, keyword, status);
        sql.append(" ORDER BY created_at DESC, tenant_id DESC LIMIT ? OFFSET ?");
        args.add(pageSize);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), this::mapTenant, args.toArray());
    }

    public long countTenants(String keyword, String status) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM sys_tenant WHERE deleted = 0");
        appendTenantFilters(sql, args, keyword, status);
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return value(count);
    }

    public TenantSummary getTenant(Long tenantId) {
        return jdbcTemplate.queryForObject("""
                SELECT tenant_id, tenant_name, tenant_code, contact_name, contact_mobile, package_id,
                       service_start_date, service_end_date, status, created_at
                FROM sys_tenant
                WHERE tenant_id = ? AND deleted = 0
                """, this::mapTenant, tenantId);
    }

    public boolean packageExists(Long packageId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_tenant_package WHERE package_id = ? AND deleted = 0",
                Integer.class,
                packageId
        );
        return count != null && count > 0;
    }

    public void insertTenant(Long tenantId, TenantRequest request) {
        jdbcTemplate.update("""
                        INSERT INTO sys_tenant(tenant_id, tenant_name, tenant_code, unified_social_credit_code,
                                               contact_name, contact_mobile, package_id, service_start_date,
                                               service_end_date, status, remark)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?)
                        """,
                tenantId,
                request.tenantName(),
                request.tenantCode(),
                request.unifiedSocialCreditCode(),
                request.contactName(),
                request.contactMobile(),
                request.packageId(),
                request.serviceStartDate(),
                request.serviceEndDate(),
                request.remark());
    }

    public void updateTenant(Long tenantId, TenantRequest request) {
        jdbcTemplate.update("""
                        UPDATE sys_tenant
                        SET tenant_name = ?, unified_social_credit_code = ?, contact_name = ?, contact_mobile = ?,
                            package_id = ?, service_start_date = ?, service_end_date = ?, remark = ?
                        WHERE tenant_id = ? AND deleted = 0
                        """,
                request.tenantName(),
                request.unifiedSocialCreditCode(),
                request.contactName(),
                request.contactMobile(),
                request.packageId(),
                request.serviceStartDate(),
                request.serviceEndDate(),
                request.remark(),
                tenantId);
    }

    public void updateTenantStatus(Long tenantId, String status, String reason) {
        jdbcTemplate.update("""
                        UPDATE sys_tenant
                        SET status = ?, remark = CONCAT(COALESCE(remark, ''), '\n状态变更：', ?)
                        WHERE tenant_id = ? AND deleted = 0
                        """, status, reason, tenantId);
    }

    public TenantConfigView getTenantConfig(Long tenantId) {
        List<TenantConfigView> configs = jdbcTemplate.query("""
                SELECT config_id, tenant_id, logo_url, service_phone, domain, wechat_appid,
                       sms_channel_code, storage_policy
                FROM sys_tenant_config
                WHERE tenant_id = ? AND deleted = 0
                """, this::mapTenantConfig, tenantId);
        return configs.stream().findFirst().orElse(null);
    }

    public void upsertTenantConfig(Long tenantId, TenantConfigRequest request) {
        TenantConfigView existing = getTenantConfig(tenantId);
        if (existing == null) {
            jdbcTemplate.update("""
                            INSERT INTO sys_tenant_config(config_id, tenant_id, logo_url, service_phone, domain,
                                                          wechat_appid, sms_channel_code, storage_policy)
                            VALUES (?, ?, ?, ?, ?, ?, ?, COALESCE(?, 'SHARED_BUCKET'))
                            """,
                    System.currentTimeMillis(),
                    tenantId,
                    request.logoUrl(),
                    request.servicePhone(),
                    request.domain(),
                    request.wechatAppid(),
                    request.smsChannelCode(),
                    request.storagePolicy());
        } else {
            jdbcTemplate.update("""
                            UPDATE sys_tenant_config
                            SET logo_url = ?, service_phone = ?, domain = ?, wechat_appid = ?,
                                sms_channel_code = ?, storage_policy = COALESCE(?, storage_policy)
                            WHERE tenant_id = ? AND deleted = 0
                            """,
                    request.logoUrl(),
                    request.servicePhone(),
                    request.domain(),
                    request.wechatAppid(),
                    request.smsChannelCode(),
                    request.storagePolicy(),
                    tenantId);
        }
    }

    public List<PackageView> findPackages(long offset, long pageSize) {
        return jdbcTemplate.query("""
                        SELECT package_id, package_name, package_code, price, billing_cycle,
                               JSON_UNQUOTE(JSON_EXTRACT(enabled_modules, '$')) AS enabled_modules,
                               JSON_UNQUOTE(JSON_EXTRACT(quotas, '$')) AS quotas,
                               status
                        FROM sys_tenant_package
                        WHERE deleted = 0
                        ORDER BY package_id ASC
                        LIMIT ? OFFSET ?
                        """,
                this::mapPackage,
                pageSize,
                offset);
    }

    public long countPackages() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_tenant_package WHERE deleted = 0", Long.class);
        return value(count);
    }

    public void insertPackage(Long packageId, PackageRequest request, String modulesJson, String quotasJson) {
        jdbcTemplate.update("""
                        INSERT INTO sys_tenant_package(package_id, package_name, package_code, price,
                                                       billing_cycle, enabled_modules, quotas, status)
                        VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVE')
                        """,
                packageId,
                request.packageName(),
                request.packageCode(),
                request.price(),
                request.billingCycle(),
                modulesJson,
                quotasJson);
    }

    public List<UsageView> findUsage(long offset, long pageSize) {
        return jdbcTemplate.query("""
                        SELECT tenant_id, stat_date, project_count, user_count, member_count, house_count,
                               storage_mb, sms_count, api_call_count
                        FROM sys_tenant_usage
                        ORDER BY stat_date DESC, tenant_id ASC
                        LIMIT ? OFFSET ?
                        """,
                this::mapUsage,
                pageSize,
                offset);
    }

    public long countUsage() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_tenant_usage", Long.class);
        return value(count);
    }

    private long count(String sql) {
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return value(count);
    }

    private void addAlert(List<MonitorAlertView> alerts, String metricCode, long count, String level, String message) {
        if (count > 0) {
            alerts.add(new MonitorAlertView(metricCode, level, count, message));
        }
    }

    private void appendTenantFilters(StringBuilder sql, List<Object> args, String keyword, String status) {
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (tenant_name LIKE ? OR tenant_code LIKE ? OR contact_mobile LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
            args.add(like);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
            args.add(status);
        }
    }

    private TenantSummary mapTenant(ResultSet rs, int rowNum) throws SQLException {
        return new TenantSummary(
                rs.getLong("tenant_id"),
                rs.getString("tenant_name"),
                rs.getString("tenant_code"),
                rs.getString("contact_name"),
                rs.getString("contact_mobile"),
                (Long) rs.getObject("package_id"),
                rs.getObject("service_start_date", java.time.LocalDate.class),
                rs.getObject("service_end_date", java.time.LocalDate.class),
                rs.getString("status"),
                rs.getTimestamp("created_at").toLocalDateTime()
        );
    }

    private TenantConfigView mapTenantConfig(ResultSet rs, int rowNum) throws SQLException {
        return new TenantConfigView(
                rs.getLong("config_id"),
                rs.getLong("tenant_id"),
                rs.getString("logo_url"),
                rs.getString("service_phone"),
                rs.getString("domain"),
                rs.getString("wechat_appid"),
                rs.getString("sms_channel_code"),
                rs.getString("storage_policy")
        );
    }

    private PackageView mapPackage(ResultSet rs, int rowNum) throws SQLException {
        return new PackageView(
                rs.getLong("package_id"),
                rs.getString("package_name"),
                rs.getString("package_code"),
                rs.getBigDecimal("price"),
                rs.getString("billing_cycle"),
                rs.getString("enabled_modules"),
                rs.getString("quotas"),
                rs.getString("status")
        );
    }

    private UsageView mapUsage(ResultSet rs, int rowNum) throws SQLException {
        return new UsageView(
                rs.getLong("tenant_id"),
                rs.getObject("stat_date", java.time.LocalDate.class),
                rs.getInt("project_count"),
                rs.getInt("user_count"),
                rs.getInt("member_count"),
                rs.getInt("house_count"),
                rs.getBigDecimal("storage_mb"),
                rs.getInt("sms_count"),
                rs.getInt("api_call_count")
        );
    }

    private long value(Long value) {
        return value == null ? 0L : value;
    }
}
