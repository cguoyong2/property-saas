package com.yongquan.propertysaas.lease.repository;

import com.yongquan.propertysaas.fee.domain.FeeBillView;
import com.yongquan.propertysaas.lease.domain.LeaseContractView;
import com.yongquan.propertysaas.lease.domain.LeaseCustomerView;
import com.yongquan.propertysaas.lease.domain.LeaseFollowRecordView;
import com.yongquan.propertysaas.lease.domain.LeaseResourceView;
import com.yongquan.propertysaas.lease.dto.LeaseContractRequest;
import com.yongquan.propertysaas.lease.dto.LeaseCustomerRequest;
import com.yongquan.propertysaas.lease.dto.LeaseFollowRequest;
import com.yongquan.propertysaas.lease.dto.LeaseResourceRequest;
import com.yongquan.propertysaas.service.domain.NoticeRecipient;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import com.yongquan.propertysaas.security.scope.ProjectScopeRepository;
import org.springframework.stereotype.Repository;

@Repository
public class LeaseRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ProjectScopeRepository projectScopeRepository;

    public LeaseRepository(JdbcTemplate jdbcTemplate, ProjectScopeRepository projectScopeRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.projectScopeRepository = projectScopeRepository;
    }

    public List<Long> findAllowedProjectIds(Long tenantId, Long userId) {
        return projectScopeRepository.findAllowedProjectIds(tenantId, userId);
    }

    public List<LeaseResourceView> findResources(Long tenantId, List<Long> allowedProjectIds, Long projectId,
                                                 String status, long offset, long pageSize) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT resource_id, project_id, resource_type, resource_name, ref_object_id, area, status, created_at
                FROM lease_resource
                WHERE tenant_id = ? AND deleted = 0
                """);
        args.add(tenantId);
        appendProjectStatus(sql, args, projectId, status);
        appendProjectScope(sql, args, allowedProjectIds, "project_id");
        sql.append(" ORDER BY created_at DESC, resource_id DESC LIMIT ? OFFSET ?");
        args.add(pageSize);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), this::mapResource, args.toArray());
    }

    public long countResources(Long tenantId, List<Long> allowedProjectIds, Long projectId, String status) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM lease_resource WHERE tenant_id = ? AND deleted = 0");
        args.add(tenantId);
        appendProjectStatus(sql, args, projectId, status);
        appendProjectScope(sql, args, allowedProjectIds, "project_id");
        return value(jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray()));
    }

    public LeaseResourceView getResource(Long tenantId, Long resourceId) {
        return jdbcTemplate.queryForObject("""
                SELECT resource_id, project_id, resource_type, resource_name, ref_object_id, area, status, created_at
                FROM lease_resource
                WHERE tenant_id = ? AND resource_id = ? AND deleted = 0
                """, this::mapResource, tenantId, resourceId);
    }

    public void insertResource(Long tenantId, Long resourceId, Long userId, LeaseResourceRequest request, String status) {
        jdbcTemplate.update("""
                        INSERT INTO lease_resource(resource_id, tenant_id, project_id, resource_type, resource_name,
                                                   ref_object_id, area, status, created_by)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """, resourceId, tenantId, request.projectId(), request.resourceType(), request.resourceName(),
                request.refObjectId(), request.area(), status, userId);
    }

    public void updateResourceStatus(Long tenantId, Long resourceId, String status, Long userId) {
        jdbcTemplate.update("""
                UPDATE lease_resource
                SET status = ?, updated_by = ?
                WHERE tenant_id = ? AND resource_id = ? AND deleted = 0
                """, status, userId, tenantId, resourceId);
    }

    public List<LeaseCustomerView> findCustomers(Long tenantId, List<Long> allowedProjectIds, Long projectId,
                                                 String status, long offset, long pageSize) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT customer_id, project_id, customer_name, contact_mobile, source_channel, demand_area,
                       budget_amount, status, owner_user_id, created_at
                FROM lease_customer
                WHERE tenant_id = ? AND deleted = 0
                """);
        args.add(tenantId);
        appendProjectStatus(sql, args, projectId, status);
        appendProjectScope(sql, args, allowedProjectIds, "project_id");
        sql.append(" ORDER BY created_at DESC, customer_id DESC LIMIT ? OFFSET ?");
        args.add(pageSize);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), this::mapCustomer, args.toArray());
    }

    public long countCustomers(Long tenantId, List<Long> allowedProjectIds, Long projectId, String status) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM lease_customer WHERE tenant_id = ? AND deleted = 0");
        args.add(tenantId);
        appendProjectStatus(sql, args, projectId, status);
        appendProjectScope(sql, args, allowedProjectIds, "project_id");
        return value(jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray()));
    }

    public LeaseCustomerView getCustomer(Long tenantId, Long customerId) {
        return jdbcTemplate.queryForObject("""
                SELECT customer_id, project_id, customer_name, contact_mobile, source_channel, demand_area,
                       budget_amount, status, owner_user_id, created_at
                FROM lease_customer
                WHERE tenant_id = ? AND customer_id = ? AND deleted = 0
                """, this::mapCustomer, tenantId, customerId);
    }

    public void insertCustomer(Long tenantId, Long customerId, Long userId, LeaseCustomerRequest request, String status) {
        jdbcTemplate.update("""
                        INSERT INTO lease_customer(customer_id, tenant_id, project_id, customer_name, contact_mobile,
                                                   source_channel, demand_area, budget_amount, status, owner_user_id, created_by)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """, customerId, tenantId, request.projectId(), request.customerName(), request.contactMobile(),
                request.sourceChannel(), request.demandArea(), request.budgetAmount(), status, request.ownerUserId(), userId);
    }

    public void insertFollow(Long tenantId, Long projectId, Long followId, Long userId, Long customerId,
                             LeaseFollowRequest request) {
        jdbcTemplate.update("""
                        INSERT INTO lease_follow_record(follow_id, tenant_id, project_id, customer_id, follow_type,
                                                        content, next_follow_at, created_by)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """, followId, tenantId, projectId, customerId, request.followType(), request.content(),
                request.nextFollowAt(), userId);
    }

    public List<LeaseFollowRecordView> findFollows(Long tenantId, Long customerId) {
        return jdbcTemplate.query("""
                SELECT follow_id, customer_id, follow_type, content, next_follow_at, created_by, created_at
                FROM lease_follow_record
                WHERE tenant_id = ? AND customer_id = ?
                ORDER BY created_at DESC, follow_id DESC
                """, this::mapFollow, tenantId, customerId);
    }

    public List<LeaseContractView> findContracts(Long tenantId, List<Long> allowedProjectIds, Long projectId,
                                                 String status, Long customerId, long offset, long pageSize) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT contract_id, project_id, contract_no, customer_id, resource_id, lessee_name, lessee_mobile,
                       start_date, end_date, rent_amount, deposit_amount, payment_cycle, free_rent_days,
                       status, attachment_file_ids, created_at
                FROM lease_contract
                WHERE tenant_id = ? AND deleted = 0
                """);
        args.add(tenantId);
        appendContractFilters(sql, args, projectId, status, customerId);
        appendProjectScope(sql, args, allowedProjectIds, "project_id");
        sql.append(" ORDER BY created_at DESC, contract_id DESC LIMIT ? OFFSET ?");
        args.add(pageSize);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), this::mapContract, args.toArray());
    }

    public long countContracts(Long tenantId, List<Long> allowedProjectIds, Long projectId, String status, Long customerId) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM lease_contract WHERE tenant_id = ? AND deleted = 0");
        args.add(tenantId);
        appendContractFilters(sql, args, projectId, status, customerId);
        appendProjectScope(sql, args, allowedProjectIds, "project_id");
        return value(jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray()));
    }

    public LeaseContractView getContract(Long tenantId, Long contractId) {
        return jdbcTemplate.queryForObject("""
                SELECT contract_id, project_id, contract_no, customer_id, resource_id, lessee_name, lessee_mobile,
                       start_date, end_date, rent_amount, deposit_amount, payment_cycle, free_rent_days,
                       status, attachment_file_ids, created_at
                FROM lease_contract
                WHERE tenant_id = ? AND contract_id = ? AND deleted = 0
                """, this::mapContract, tenantId, contractId);
    }

    public void insertContract(Long tenantId, Long contractId, String contractNo, Long userId,
                               LeaseContractRequest request, String status) {
        jdbcTemplate.update("""
                        INSERT INTO lease_contract(contract_id, tenant_id, project_id, contract_no, customer_id,
                                                   resource_id, lessee_name, lessee_mobile, start_date, end_date,
                                                   rent_amount, deposit_amount, payment_cycle, free_rent_days,
                                                   status, attachment_file_ids, created_by)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """, contractId, tenantId, request.projectId(), contractNo, request.customerId(),
                request.resourceId(), request.lesseeName(), request.lesseeMobile(), request.startDate(), request.endDate(),
                request.rentAmount(), request.depositAmount(), request.paymentCycle(), request.freeRentDays(),
                status, request.attachmentFileIds(), userId);
    }

    public int updateContractStatus(Long tenantId, Long contractId, String fromStatus, String toStatus, Long userId) {
        return jdbcTemplate.update("""
                UPDATE lease_contract
                SET status = ?, updated_by = ?
                WHERE tenant_id = ? AND contract_id = ? AND deleted = 0 AND status = ?
                """, toStatus, userId, tenantId, contractId, fromStatus);
    }

    public List<FeeBillView> findContractBills(Long tenantId, Long contractId) {
        return jdbcTemplate.query("""
                SELECT bill_id, project_id, bill_no, item_id, standard_id, object_type, object_id, member_id,
                       house_id, bill_period, receivable_amount, discount_amount, paid_amount, refund_amount,
                       remaining_amount, due_date, status, source_type, void_reason, created_at
                FROM fee_bill
                WHERE tenant_id = ? AND object_type = 'CONTRACT' AND object_id = ? AND deleted = 0
                ORDER BY bill_period ASC, bill_id ASC
                """, this::mapBill, tenantId, contractId);
    }

    public boolean billExists(Long tenantId, Long contractId, String billPeriod) {
        return exists("""
                SELECT COUNT(*) FROM fee_bill
                WHERE tenant_id = ? AND object_type = 'CONTRACT' AND object_id = ?
                  AND bill_period = ? AND deleted = 0 AND status <> 'VOID'
                """, tenantId, contractId, billPeriod);
    }

    public void insertRentBill(Long tenantId, Long billId, String billNo, Long userId, Long projectId,
                               Long itemId, Long contractId, String billPeriod, BigDecimal amount, LocalDate dueDate) {
        jdbcTemplate.update("""
                        INSERT INTO fee_bill(bill_id, tenant_id, project_id, bill_no, item_id, object_type, object_id,
                                             bill_period, receivable_amount, discount_amount, paid_amount, refund_amount,
                                             remaining_amount, due_date, status, source_type, created_by)
                        VALUES (?, ?, ?, ?, ?, 'CONTRACT', ?, ?, ?, 0.00, 0.00, 0.00, ?, ?, 'UNPAID', 'LEASE_CONTRACT', ?)
                        """, billId, tenantId, projectId, billNo, itemId, contractId, billPeriod, amount, amount, dueDate, userId);
    }

    public Long findOrCreateRentItem(Long tenantId, Long userId, Long newItemId) {
        List<Long> ids = jdbcTemplate.queryForList("""
                SELECT item_id FROM fee_item
                WHERE tenant_id = ? AND item_code = 'LEASE_RENT' AND deleted = 0
                ORDER BY item_id ASC LIMIT 1
                """, Long.class, tenantId);
        if (!ids.isEmpty()) {
            return ids.get(0);
        }
        jdbcTemplate.update("""
                INSERT INTO fee_item(item_id, tenant_id, item_code, item_name, item_type, status, created_by)
                VALUES (?, ?, 'LEASE_RENT', '租金', 'PERIODIC', 'ACTIVE', ?)
                """, newItemId, tenantId, userId);
        return newItemId;
    }

    public void insertMessage(Long messageId, Long tenantId, Long projectId, NoticeRecipient recipient,
                              String channel, String templateCode, String title, String content) {
        jdbcTemplate.update("""
                        INSERT INTO message_record(message_id, tenant_id, project_id, receiver_type, receiver_id,
                                                   receiver_mobile, channel, template_code, title, content, send_status)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING')
                        """, messageId, tenantId, projectId, recipient.receiverType(), recipient.receiverId(),
                recipient.receiverMobile(), channel, templateCode, title, content);
    }

    public List<NoticeRecipient> findProjectUserRecipients(Long tenantId, Long projectId) {
        return jdbcTemplate.query("""
                SELECT 'USER' AS receiver_type, u.user_id AS receiver_id, u.mobile AS receiver_mobile
                FROM sys_user u
                WHERE u.tenant_id = ? AND u.status = 'ACTIVE' AND u.deleted = 0
                  AND (
                    EXISTS (
                      SELECT 1 FROM sys_user_role ur
                      JOIN sys_role r ON r.role_id = ur.role_id
                      WHERE ur.tenant_id = u.tenant_id AND ur.user_id = u.user_id
                        AND r.tenant_id = u.tenant_id AND r.status = 'ACTIVE' AND r.data_scope = 'ALL_TENANT'
                    )
                    OR EXISTS (
                      SELECT 1 FROM sys_user_project p
                      WHERE p.tenant_id = u.tenant_id AND p.user_id = u.user_id AND p.project_id = ?
                    )
                  )
                ORDER BY u.user_id ASC
                """, this::mapRecipient, tenantId, projectId);
    }

    public boolean projectExists(Long tenantId, Long projectId) {
        return exists("SELECT COUNT(*) FROM base_project WHERE tenant_id = ? AND project_id = ? AND deleted = 0",
                tenantId, projectId);
    }

    public boolean userExists(Long tenantId, Long userId) {
        return exists("SELECT COUNT(*) FROM sys_user WHERE tenant_id = ? AND user_id = ? AND status = 'ACTIVE' AND deleted = 0",
                tenantId, userId);
    }

    private void appendProjectStatus(StringBuilder sql, List<Object> args, Long projectId, String status) {
        if (projectId != null) {
            sql.append(" AND project_id = ?");
            args.add(projectId);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
            args.add(status);
        }
    }

    private void appendContractFilters(StringBuilder sql, List<Object> args, Long projectId, String status, Long customerId) {
        appendProjectStatus(sql, args, projectId, status);
        if (customerId != null) {
            sql.append(" AND customer_id = ?");
            args.add(customerId);
        }
    }

    private void appendProjectScope(StringBuilder sql, List<Object> args, List<Long> allowedProjectIds, String column) {
        if (allowedProjectIds == null) {
            return;
        }
        if (allowedProjectIds.isEmpty()) {
            sql.append(" AND 1 = 0");
            return;
        }
        sql.append(" AND ").append(column).append(" IN (");
        sql.append("?,".repeat(allowedProjectIds.size()));
        sql.setLength(sql.length() - 1);
        sql.append(")");
        args.addAll(allowedProjectIds);
    }

    private boolean exists(String sql, Object... args) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return count != null && count > 0;
    }

    private LeaseResourceView mapResource(ResultSet rs, int rowNum) throws SQLException {
        return new LeaseResourceView(rs.getLong("resource_id"), rs.getLong("project_id"), rs.getString("resource_type"),
                rs.getString("resource_name"), (Long) rs.getObject("ref_object_id"), rs.getBigDecimal("area"),
                rs.getString("status"), rs.getTimestamp("created_at").toLocalDateTime());
    }

    private LeaseCustomerView mapCustomer(ResultSet rs, int rowNum) throws SQLException {
        return new LeaseCustomerView(rs.getLong("customer_id"), rs.getLong("project_id"), rs.getString("customer_name"),
                rs.getString("contact_mobile"), rs.getString("source_channel"), rs.getBigDecimal("demand_area"),
                rs.getBigDecimal("budget_amount"), rs.getString("status"), (Long) rs.getObject("owner_user_id"),
                rs.getTimestamp("created_at").toLocalDateTime());
    }

    private LeaseFollowRecordView mapFollow(ResultSet rs, int rowNum) throws SQLException {
        return new LeaseFollowRecordView(rs.getLong("follow_id"), rs.getLong("customer_id"), rs.getString("follow_type"),
                rs.getString("content"), rs.getTimestamp("next_follow_at") == null ? null : rs.getTimestamp("next_follow_at").toLocalDateTime(),
                (Long) rs.getObject("created_by"), rs.getTimestamp("created_at").toLocalDateTime());
    }

    private LeaseContractView mapContract(ResultSet rs, int rowNum) throws SQLException {
        return new LeaseContractView(rs.getLong("contract_id"), rs.getLong("project_id"), rs.getString("contract_no"),
                (Long) rs.getObject("customer_id"), rs.getLong("resource_id"), rs.getString("lessee_name"),
                rs.getString("lessee_mobile"), rs.getObject("start_date", LocalDate.class),
                rs.getObject("end_date", LocalDate.class), rs.getBigDecimal("rent_amount"),
                rs.getBigDecimal("deposit_amount"), rs.getString("payment_cycle"), rs.getInt("free_rent_days"),
                rs.getString("status"), rs.getString("attachment_file_ids"), rs.getTimestamp("created_at").toLocalDateTime());
    }

    private FeeBillView mapBill(ResultSet rs, int rowNum) throws SQLException {
        return new FeeBillView(rs.getLong("bill_id"), rs.getLong("project_id"), rs.getString("bill_no"),
                rs.getLong("item_id"), (Long) rs.getObject("standard_id"), rs.getString("object_type"),
                rs.getLong("object_id"), (Long) rs.getObject("member_id"), (Long) rs.getObject("house_id"),
                rs.getString("bill_period"), rs.getBigDecimal("receivable_amount"), rs.getBigDecimal("discount_amount"),
                rs.getBigDecimal("paid_amount"), rs.getBigDecimal("refund_amount"), rs.getBigDecimal("remaining_amount"),
                rs.getObject("due_date", LocalDate.class), rs.getString("status"), rs.getString("source_type"),
                rs.getString("void_reason"), rs.getTimestamp("created_at").toLocalDateTime(),
                null, null, null, null, null, null, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null);
    }

    private NoticeRecipient mapRecipient(ResultSet rs, int rowNum) throws SQLException {
        return new NoticeRecipient(rs.getString("receiver_type"), (Long) rs.getObject("receiver_id"),
                rs.getString("receiver_mobile"));
    }

    private long value(Long value) {
        return value == null ? 0L : value;
    }
}
