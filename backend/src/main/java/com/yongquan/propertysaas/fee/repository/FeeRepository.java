package com.yongquan.propertysaas.fee.repository;

import com.yongquan.propertysaas.fee.domain.FeeItemView;
import com.yongquan.propertysaas.fee.domain.FeeStandardBindView;
import com.yongquan.propertysaas.fee.domain.FeeStandardView;
import com.yongquan.propertysaas.fee.dto.FeeItemRequest;
import com.yongquan.propertysaas.fee.dto.FeeStandardBindRequest;
import com.yongquan.propertysaas.fee.dto.FeeStandardRequest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import com.yongquan.propertysaas.security.scope.ProjectScopeRepository;
import org.springframework.stereotype.Repository;

@Repository
public class FeeRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ProjectScopeRepository projectScopeRepository;

    public FeeRepository(JdbcTemplate jdbcTemplate, ProjectScopeRepository projectScopeRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.projectScopeRepository = projectScopeRepository;
    }

    public List<Long> findAllowedProjectIds(Long tenantId, Long userId) {
        return projectScopeRepository.findAllowedProjectIds(tenantId, userId);
    }

    public List<FeeItemView> findItems(Long tenantId, String keyword, String status, long offset, long pageSize) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT item_id, item_code, item_name, item_type, status, created_at
                FROM fee_item
                WHERE tenant_id = ? AND deleted = 0
                """);
        args.add(tenantId);
        appendKeyword(sql, args, keyword, "item_code", "item_name");
        appendStatus(sql, args, status);
        sql.append(" ORDER BY created_at DESC, item_id DESC LIMIT ? OFFSET ?");
        args.add(pageSize);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), this::mapItem, args.toArray());
    }

    public long countItems(Long tenantId, String keyword, String status) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM fee_item WHERE tenant_id = ? AND deleted = 0");
        args.add(tenantId);
        appendKeyword(sql, args, keyword, "item_code", "item_name");
        appendStatus(sql, args, status);
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return value(count);
    }

    public FeeItemView getItem(Long tenantId, Long itemId) {
        return jdbcTemplate.queryForObject("""
                SELECT item_id, item_code, item_name, item_type, status, created_at
                FROM fee_item
                WHERE tenant_id = ? AND item_id = ? AND deleted = 0
                """, this::mapItem, tenantId, itemId);
    }

    public void insertItem(Long tenantId, Long itemId, Long userId, String itemCode, FeeItemRequest request) {
        jdbcTemplate.update("""
                        INSERT INTO fee_item(item_id, tenant_id, item_code, item_name, item_type, status, created_by)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
                itemId, tenantId, itemCode, request.itemName(), request.itemType(),
                text(request.status(), "ACTIVE"), userId);
    }

    public void updateItem(Long tenantId, Long itemId, Long userId, FeeItemRequest request) {
        jdbcTemplate.update("""
                        UPDATE fee_item
                        SET item_name = ?, item_type = ?, status = ?, updated_by = ?
                        WHERE tenant_id = ? AND item_id = ? AND deleted = 0
                        """,
                request.itemName(), request.itemType(), text(request.status(), "ACTIVE"), userId, tenantId, itemId);
    }

    public List<FeeStandardView> findStandards(Long tenantId, List<Long> allowedProjectIds, Long projectId, Long itemId,
                                               String status, long offset, long pageSize) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT standard_id, project_id, item_id, standard_name, charge_method, unit_price, cycle,
                       formula, effective_date, expire_date, status, created_at
                FROM fee_standard
                WHERE tenant_id = ? AND deleted = 0
                """);
        args.add(tenantId);
        appendProjectFilter(sql, args, projectId);
        if (itemId != null) {
            sql.append(" AND item_id = ?");
            args.add(itemId);
        }
        appendStatus(sql, args, status);
        appendNullableProjectScope(sql, args, allowedProjectIds);
        sql.append(" ORDER BY created_at DESC, standard_id DESC LIMIT ? OFFSET ?");
        args.add(pageSize);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), this::mapStandard, args.toArray());
    }

    public long countStandards(Long tenantId, List<Long> allowedProjectIds, Long projectId, Long itemId, String status) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM fee_standard WHERE tenant_id = ? AND deleted = 0");
        args.add(tenantId);
        appendProjectFilter(sql, args, projectId);
        if (itemId != null) {
            sql.append(" AND item_id = ?");
            args.add(itemId);
        }
        appendStatus(sql, args, status);
        appendNullableProjectScope(sql, args, allowedProjectIds);
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return value(count);
    }

    public FeeStandardView getStandard(Long tenantId, Long standardId) {
        return jdbcTemplate.queryForObject("""
                SELECT standard_id, project_id, item_id, standard_name, charge_method, unit_price, cycle,
                       formula, effective_date, expire_date, status, created_at
                FROM fee_standard
                WHERE tenant_id = ? AND standard_id = ? AND deleted = 0
                """, this::mapStandard, tenantId, standardId);
    }

    public void insertStandard(Long tenantId, Long standardId, Long userId, FeeStandardRequest request) {
        jdbcTemplate.update("""
                        INSERT INTO fee_standard(standard_id, tenant_id, project_id, item_id, standard_name,
                                                 charge_method, unit_price, cycle, formula, effective_date,
                                                 expire_date, status, created_by)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                standardId, tenantId, request.projectId(), request.itemId(), request.standardName(),
                request.chargeMethod(), request.unitPrice(), text(request.cycle(), "MONTH"), request.formula(),
                request.effectiveDate(), request.expireDate(), text(request.status(), "ACTIVE"), userId);
    }

    public void updateStandard(Long tenantId, Long standardId, Long userId, FeeStandardRequest request) {
        jdbcTemplate.update("""
                        UPDATE fee_standard
                        SET project_id = ?, item_id = ?, standard_name = ?, charge_method = ?, unit_price = ?,
                            cycle = ?, formula = ?, effective_date = ?, expire_date = ?, status = ?, updated_by = ?
                        WHERE tenant_id = ? AND standard_id = ? AND deleted = 0
                        """,
                request.projectId(), request.itemId(), request.standardName(), request.chargeMethod(), request.unitPrice(),
                text(request.cycle(), "MONTH"), request.formula(), request.effectiveDate(), request.expireDate(),
                text(request.status(), "ACTIVE"), userId, tenantId, standardId);
    }

    public List<FeeStandardBindView> findBinds(Long tenantId, List<Long> allowedProjectIds, Long projectId, Long standardId,
                                               String objectType, String status, long offset, long pageSize) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT bind_id, project_id, standard_id, object_type, object_id, effective_date, expire_date,
                       status, created_at
                FROM fee_standard_bind
                WHERE tenant_id = ? AND deleted = 0
                """);
        args.add(tenantId);
        appendProjectFilter(sql, args, projectId);
        if (standardId != null) {
            sql.append(" AND standard_id = ?");
            args.add(standardId);
        }
        if (objectType != null && !objectType.isBlank()) {
            sql.append(" AND object_type = ?");
            args.add(objectType);
        }
        appendStatus(sql, args, status);
        appendProjectScope(sql, args, allowedProjectIds);
        sql.append(" ORDER BY created_at DESC, bind_id DESC LIMIT ? OFFSET ?");
        args.add(pageSize);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), this::mapBind, args.toArray());
    }

    public long countBinds(Long tenantId, List<Long> allowedProjectIds, Long projectId, Long standardId,
                           String objectType, String status) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM fee_standard_bind WHERE tenant_id = ? AND deleted = 0");
        args.add(tenantId);
        appendProjectFilter(sql, args, projectId);
        if (standardId != null) {
            sql.append(" AND standard_id = ?");
            args.add(standardId);
        }
        if (objectType != null && !objectType.isBlank()) {
            sql.append(" AND object_type = ?");
            args.add(objectType);
        }
        appendStatus(sql, args, status);
        appendProjectScope(sql, args, allowedProjectIds);
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return value(count);
    }

    public void insertBind(Long tenantId, Long bindId, Long userId, FeeStandardBindRequest request) {
        jdbcTemplate.update("""
                        INSERT INTO fee_standard_bind(bind_id, tenant_id, project_id, standard_id, object_type,
                                                      object_id, effective_date, expire_date, status, created_by)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                bindId, tenantId, request.projectId(), request.standardId(), request.objectType(), request.objectId(),
                request.effectiveDate(), request.expireDate(), text(request.status(), "ACTIVE"), userId);
    }

    public boolean itemExists(Long tenantId, Long itemId) {
        return exists("SELECT COUNT(*) FROM fee_item WHERE tenant_id = ? AND item_id = ? AND deleted = 0", tenantId, itemId);
    }

    public boolean projectExists(Long tenantId, Long projectId) {
        if (projectId == null) {
            return true;
        }
        return exists("SELECT COUNT(*) FROM base_project WHERE tenant_id = ? AND project_id = ? AND deleted = 0", tenantId, projectId);
    }

    public boolean standardExists(Long tenantId, Long standardId) {
        return exists("SELECT COUNT(*) FROM fee_standard WHERE tenant_id = ? AND standard_id = ? AND deleted = 0", tenantId, standardId);
    }

    public boolean bindObjectExists(Long tenantId, Long projectId, String objectType, Long objectId) {
        return switch (objectType) {
            case "HOUSE" -> exists("SELECT COUNT(*) FROM base_house WHERE tenant_id = ? AND project_id = ? AND house_id = ? AND deleted = 0",
                    tenantId, projectId, objectId);
            case "VEHICLE" -> exists("SELECT COUNT(*) FROM base_vehicle WHERE tenant_id = ? AND project_id = ? AND vehicle_id = ? AND deleted = 0",
                    tenantId, projectId, objectId);
            case "SPACE" -> exists("SELECT COUNT(*) FROM base_parking_space WHERE tenant_id = ? AND project_id = ? AND space_id = ? AND deleted = 0",
                    tenantId, projectId, objectId);
            default -> false;
        };
    }

    private void appendNullableProjectScope(StringBuilder sql, List<Object> args, List<Long> allowedProjectIds) {
        if (allowedProjectIds == null) {
            return;
        }
        if (allowedProjectIds.isEmpty()) {
            sql.append(" AND project_id IS NULL");
            return;
        }
        sql.append(" AND (project_id IS NULL OR project_id IN (");
        sql.append("?,".repeat(allowedProjectIds.size()));
        sql.setLength(sql.length() - 1);
        sql.append("))");
        args.addAll(allowedProjectIds);
    }

    private void appendProjectScope(StringBuilder sql, List<Object> args, List<Long> allowedProjectIds) {
        if (allowedProjectIds == null) {
            return;
        }
        if (allowedProjectIds.isEmpty()) {
            sql.append(" AND 1 = 0");
            return;
        }
        sql.append(" AND project_id IN (");
        sql.append("?,".repeat(allowedProjectIds.size()));
        sql.setLength(sql.length() - 1);
        sql.append(")");
        args.addAll(allowedProjectIds);
    }

    private void appendProjectFilter(StringBuilder sql, List<Object> args, Long projectId) {
        if (projectId != null) {
            sql.append(" AND project_id = ?");
            args.add(projectId);
        }
    }

    private void appendKeyword(StringBuilder sql, List<Object> args, String keyword, String fieldA, String fieldB) {
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (").append(fieldA).append(" LIKE ? OR ").append(fieldB).append(" LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
        }
    }

    private void appendStatus(StringBuilder sql, List<Object> args, String status) {
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
            args.add(status);
        }
    }

    private boolean exists(String sql, Object... args) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return count != null && count > 0;
    }

    private FeeItemView mapItem(ResultSet rs, int rowNum) throws SQLException {
        return new FeeItemView(rs.getLong("item_id"), rs.getString("item_code"), rs.getString("item_name"),
                rs.getString("item_type"), rs.getString("status"), rs.getTimestamp("created_at").toLocalDateTime());
    }

    private FeeStandardView mapStandard(ResultSet rs, int rowNum) throws SQLException {
        return new FeeStandardView(rs.getLong("standard_id"), (Long) rs.getObject("project_id"),
                rs.getLong("item_id"), rs.getString("standard_name"), rs.getString("charge_method"),
                rs.getBigDecimal("unit_price"), rs.getString("cycle"), rs.getString("formula"),
                rs.getObject("effective_date", java.time.LocalDate.class),
                rs.getObject("expire_date", java.time.LocalDate.class),
                rs.getString("status"), rs.getTimestamp("created_at").toLocalDateTime());
    }

    private FeeStandardBindView mapBind(ResultSet rs, int rowNum) throws SQLException {
        return new FeeStandardBindView(rs.getLong("bind_id"), rs.getLong("project_id"), rs.getLong("standard_id"),
                rs.getString("object_type"), rs.getLong("object_id"),
                rs.getObject("effective_date", java.time.LocalDate.class),
                rs.getObject("expire_date", java.time.LocalDate.class),
                rs.getString("status"), rs.getTimestamp("created_at").toLocalDateTime());
    }

    private String text(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private long value(Long value) {
        return value == null ? 0L : value;
    }
}
