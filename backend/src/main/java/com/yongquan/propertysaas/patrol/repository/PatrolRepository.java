package com.yongquan.propertysaas.patrol.repository;

import com.yongquan.propertysaas.patrol.domain.AssetEquipmentView;
import com.yongquan.propertysaas.patrol.domain.PatrolPlanView;
import com.yongquan.propertysaas.patrol.domain.PatrolPointView;
import com.yongquan.propertysaas.patrol.domain.PatrolTaskItemView;
import com.yongquan.propertysaas.patrol.domain.PatrolTaskView;
import com.yongquan.propertysaas.patrol.dto.AssetEquipmentRequest;
import com.yongquan.propertysaas.patrol.dto.PatrolPlanRequest;
import com.yongquan.propertysaas.patrol.dto.PatrolPointRequest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import com.yongquan.propertysaas.security.scope.ProjectScopeRepository;
import org.springframework.stereotype.Repository;

@Repository
public class PatrolRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ProjectScopeRepository projectScopeRepository;

    public PatrolRepository(JdbcTemplate jdbcTemplate, ProjectScopeRepository projectScopeRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.projectScopeRepository = projectScopeRepository;
    }

    public List<Long> findAllowedProjectIds(Long tenantId, Long userId) {
        return projectScopeRepository.findAllowedProjectIds(tenantId, userId);
    }

    public List<AssetEquipmentView> findEquipments(Long tenantId, List<Long> allowedProjectIds, Long projectId,
                                                   String status, long offset, long pageSize) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT equipment_id, project_id, equipment_code, equipment_name, equipment_type, location,
                       responsible_user_id, status, created_at
                FROM asset_equipment
                WHERE tenant_id = ? AND deleted = 0
                """);
        args.add(tenantId);
        appendProjectStatus(sql, args, projectId, status);
        appendProjectScope(sql, args, allowedProjectIds, "project_id");
        sql.append(" ORDER BY created_at DESC, equipment_id DESC LIMIT ? OFFSET ?");
        args.add(pageSize);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), this::mapEquipment, args.toArray());
    }

    public long countEquipments(Long tenantId, List<Long> allowedProjectIds, Long projectId, String status) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM asset_equipment WHERE tenant_id = ? AND deleted = 0");
        args.add(tenantId);
        appendProjectStatus(sql, args, projectId, status);
        appendProjectScope(sql, args, allowedProjectIds, "project_id");
        return value(jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray()));
    }

    public void insertEquipment(Long tenantId, Long equipmentId, Long userId, AssetEquipmentRequest request, String status) {
        jdbcTemplate.update("""
                        INSERT INTO asset_equipment(equipment_id, tenant_id, project_id, equipment_code, equipment_name,
                                                    equipment_type, location, responsible_user_id, status, created_by)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """, equipmentId, tenantId, request.projectId(), request.equipmentCode(),
                request.equipmentName(), request.equipmentType(), request.location(), request.responsibleUserId(),
                status, userId);
    }

    public List<PatrolPointView> findPoints(Long tenantId, List<Long> allowedProjectIds, Long projectId,
                                            String status, long offset, long pageSize) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT point_id, project_id, point_code, point_name, point_type, equipment_id, location,
                       qr_code, nfc_code, status, created_at
                FROM patrol_point
                WHERE tenant_id = ? AND deleted = 0
                """);
        args.add(tenantId);
        appendProjectStatus(sql, args, projectId, status);
        appendProjectScope(sql, args, allowedProjectIds, "project_id");
        sql.append(" ORDER BY created_at DESC, point_id DESC LIMIT ? OFFSET ?");
        args.add(pageSize);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), this::mapPoint, args.toArray());
    }

    public long countPoints(Long tenantId, List<Long> allowedProjectIds, Long projectId, String status) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM patrol_point WHERE tenant_id = ? AND deleted = 0");
        args.add(tenantId);
        appendProjectStatus(sql, args, projectId, status);
        appendProjectScope(sql, args, allowedProjectIds, "project_id");
        return value(jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray()));
    }

    public void insertPoint(Long tenantId, Long pointId, Long userId, PatrolPointRequest request, String status) {
        jdbcTemplate.update("""
                        INSERT INTO patrol_point(point_id, tenant_id, project_id, point_code, point_name, point_type,
                                                 equipment_id, location, qr_code, nfc_code, status, created_by)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """, pointId, tenantId, request.projectId(), request.pointCode(), request.pointName(),
                request.pointType(), request.equipmentId(), request.location(), request.qrCode(), request.nfcCode(),
                status, userId);
    }

    public List<PatrolPlanView> findPlans(Long tenantId, List<Long> allowedProjectIds, Long projectId,
                                          String status, long offset, long pageSize) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT plan_id, project_id, plan_name, cycle_type, executor_user_id, start_date, end_date,
                       status, created_at
                FROM patrol_plan
                WHERE tenant_id = ? AND deleted = 0
                """);
        args.add(tenantId);
        appendProjectStatus(sql, args, projectId, status);
        appendProjectScope(sql, args, allowedProjectIds, "project_id");
        sql.append(" ORDER BY created_at DESC, plan_id DESC LIMIT ? OFFSET ?");
        args.add(pageSize);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), this::mapPlan, args.toArray());
    }

    public long countPlans(Long tenantId, List<Long> allowedProjectIds, Long projectId, String status) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM patrol_plan WHERE tenant_id = ? AND deleted = 0");
        args.add(tenantId);
        appendProjectStatus(sql, args, projectId, status);
        appendProjectScope(sql, args, allowedProjectIds, "project_id");
        return value(jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray()));
    }

    public void insertPlan(Long tenantId, Long planId, Long userId, PatrolPlanRequest request, String status) {
        jdbcTemplate.update("""
                        INSERT INTO patrol_plan(plan_id, tenant_id, project_id, plan_name, cycle_type,
                                                executor_user_id, start_date, end_date, status, created_by)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """, planId, tenantId, request.projectId(), request.planName(), request.cycleType(),
                request.executorUserId(), request.startDate(), request.endDate(), status, userId);
    }

    public PatrolPlanView getPlan(Long tenantId, Long planId) {
        return jdbcTemplate.queryForObject("""
                SELECT plan_id, project_id, plan_name, cycle_type, executor_user_id, start_date, end_date,
                       status, created_at
                FROM patrol_plan
                WHERE tenant_id = ? AND plan_id = ? AND deleted = 0
                """, this::mapPlan, tenantId, planId);
    }

    public List<PatrolTaskView> findTasks(Long tenantId, List<Long> allowedProjectIds, Long projectId,
                                          String status, Long executorUserId, long offset, long pageSize) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT task_id, project_id, plan_id, task_no, task_name, executor_user_id, planned_start_at,
                       planned_end_at, actual_start_at, actual_end_at, status, created_at
                FROM patrol_task
                WHERE tenant_id = ? AND deleted = 0
                """);
        args.add(tenantId);
        appendTaskFilters(sql, args, projectId, status, executorUserId);
        appendProjectScope(sql, args, allowedProjectIds, "project_id");
        sql.append(" ORDER BY planned_start_at DESC, task_id DESC LIMIT ? OFFSET ?");
        args.add(pageSize);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), this::mapTask, args.toArray());
    }

    public long countTasks(Long tenantId, List<Long> allowedProjectIds, Long projectId, String status, Long executorUserId) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM patrol_task WHERE tenant_id = ? AND deleted = 0");
        args.add(tenantId);
        appendTaskFilters(sql, args, projectId, status, executorUserId);
        appendProjectScope(sql, args, allowedProjectIds, "project_id");
        return value(jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray()));
    }

    public PatrolTaskView getTask(Long tenantId, Long taskId) {
        return jdbcTemplate.queryForObject("""
                SELECT task_id, project_id, plan_id, task_no, task_name, executor_user_id, planned_start_at,
                       planned_end_at, actual_start_at, actual_end_at, status, created_at
                FROM patrol_task
                WHERE tenant_id = ? AND task_id = ? AND deleted = 0
                """, this::mapTask, tenantId, taskId);
    }

    public void insertTask(Long tenantId, Long taskId, Long userId, Long projectId, Long planId, String taskNo,
                           String taskName, Long executorUserId, LocalDateTime plannedStartAt, LocalDateTime plannedEndAt) {
        jdbcTemplate.update("""
                        INSERT INTO patrol_task(task_id, tenant_id, project_id, plan_id, task_no, task_name,
                                                executor_user_id, planned_start_at, planned_end_at, status, created_by)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', ?)
                        """, taskId, tenantId, projectId, planId, taskNo, taskName, executorUserId,
                plannedStartAt, plannedEndAt, userId);
    }

    public void insertTaskItem(Long tenantId, Long itemId, Long projectId, Long taskId, Long pointId) {
        jdbcTemplate.update("""
                        INSERT INTO patrol_task_item(item_id, tenant_id, project_id, task_id, point_id, status)
                        VALUES (?, ?, ?, ?, ?, 'PENDING')
                        """, itemId, tenantId, projectId, taskId, pointId);
    }

    public List<PatrolTaskItemView> findTaskItems(Long tenantId, Long taskId) {
        return jdbcTemplate.query("""
                SELECT item_id, task_id, point_id, result, content, image_file_ids, checked_at, status, created_at
                FROM patrol_task_item
                WHERE tenant_id = ? AND task_id = ?
                ORDER BY item_id ASC
                """, this::mapTaskItem, tenantId, taskId);
    }

    public int updateTaskStart(Long tenantId, Long taskId) {
        return jdbcTemplate.update("""
                UPDATE patrol_task
                SET status = 'IN_PROGRESS', actual_start_at = COALESCE(actual_start_at, NOW())
                WHERE tenant_id = ? AND task_id = ? AND status = 'PENDING' AND deleted = 0
                """, tenantId, taskId);
    }

    public void updateTaskItem(Long tenantId, Long itemId, String result, String status, String content, String imageFileIds) {
        jdbcTemplate.update("""
                UPDATE patrol_task_item
                SET result = ?, status = ?, content = ?, image_file_ids = ?, checked_at = NOW()
                WHERE tenant_id = ? AND item_id = ?
                """, result, status, content, imageFileIds, tenantId, itemId);
    }

    public void updateTaskStatus(Long tenantId, Long taskId, String status, boolean end) {
        jdbcTemplate.update("""
                UPDATE patrol_task
                SET status = ?, actual_end_at = IF(?, COALESCE(actual_end_at, NOW()), actual_end_at)
                WHERE tenant_id = ? AND task_id = ? AND deleted = 0
                """, status, end, tenantId, taskId);
    }

    public void updateTaskItemStatus(Long tenantId, Long itemId, String status, String content, String imageFileIds) {
        jdbcTemplate.update("""
                UPDATE patrol_task_item
                SET status = ?, content = COALESCE(?, content), image_file_ids = COALESCE(?, image_file_ids),
                    checked_at = COALESCE(checked_at, NOW())
                WHERE tenant_id = ? AND item_id = ?
                """, status, content, imageFileIds, tenantId, itemId);
    }

    public List<PatrolTaskView> findMissedCandidates(Long tenantId, List<Long> allowedProjectIds, int limit) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT task_id, project_id, plan_id, task_no, task_name, executor_user_id, planned_start_at,
                       planned_end_at, actual_start_at, actual_end_at, status, created_at
                FROM patrol_task
                WHERE tenant_id = ? AND deleted = 0 AND status = 'PENDING' AND planned_end_at < NOW()
                """);
        args.add(tenantId);
        appendProjectScope(sql, args, allowedProjectIds, "project_id");
        sql.append(" ORDER BY planned_end_at ASC LIMIT ?");
        args.add(limit);
        return jdbcTemplate.query(sql.toString(), this::mapTask, args.toArray());
    }

    public void markTaskItemsMissed(Long tenantId, Long taskId) {
        jdbcTemplate.update("""
                UPDATE patrol_task_item
                SET result = 'MISSED', status = 'MISSED'
                WHERE tenant_id = ? AND task_id = ? AND status = 'PENDING'
                """, tenantId, taskId);
    }

    public boolean projectExists(Long tenantId, Long projectId) {
        return exists("SELECT COUNT(*) FROM base_project WHERE tenant_id = ? AND project_id = ? AND deleted = 0",
                tenantId, projectId);
    }

    public boolean userExists(Long tenantId, Long userId) {
        return exists("SELECT COUNT(*) FROM sys_user WHERE tenant_id = ? AND user_id = ? AND status = 'ACTIVE' AND deleted = 0",
                tenantId, userId);
    }

    public boolean equipmentExists(Long tenantId, Long projectId, Long equipmentId) {
        return exists("""
                SELECT COUNT(*) FROM asset_equipment
                WHERE tenant_id = ? AND project_id = ? AND equipment_id = ? AND deleted = 0
                """, tenantId, projectId, equipmentId);
    }

    public boolean pointExists(Long tenantId, Long projectId, Long pointId) {
        return exists("""
                SELECT COUNT(*) FROM patrol_point
                WHERE tenant_id = ? AND project_id = ? AND point_id = ? AND deleted = 0 AND status = 'ACTIVE'
                """, tenantId, projectId, pointId);
    }

    public boolean taskItemExists(Long tenantId, Long taskId, Long itemId) {
        return exists("""
                SELECT COUNT(*) FROM patrol_task_item
                WHERE tenant_id = ? AND task_id = ? AND item_id = ?
                """, tenantId, taskId, itemId);
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

    private void appendTaskFilters(StringBuilder sql, List<Object> args, Long projectId, String status, Long executorUserId) {
        appendProjectStatus(sql, args, projectId, status);
        if (executorUserId != null) {
            sql.append(" AND executor_user_id = ?");
            args.add(executorUserId);
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

    private AssetEquipmentView mapEquipment(ResultSet rs, int rowNum) throws SQLException {
        return new AssetEquipmentView(rs.getLong("equipment_id"), rs.getLong("project_id"),
                rs.getString("equipment_code"), rs.getString("equipment_name"), rs.getString("equipment_type"),
                rs.getString("location"), (Long) rs.getObject("responsible_user_id"), rs.getString("status"),
                rs.getTimestamp("created_at").toLocalDateTime());
    }

    private PatrolPointView mapPoint(ResultSet rs, int rowNum) throws SQLException {
        return new PatrolPointView(rs.getLong("point_id"), rs.getLong("project_id"), rs.getString("point_code"),
                rs.getString("point_name"), rs.getString("point_type"), (Long) rs.getObject("equipment_id"),
                rs.getString("location"), rs.getString("qr_code"), rs.getString("nfc_code"), rs.getString("status"),
                rs.getTimestamp("created_at").toLocalDateTime());
    }

    private PatrolPlanView mapPlan(ResultSet rs, int rowNum) throws SQLException {
        return new PatrolPlanView(rs.getLong("plan_id"), rs.getLong("project_id"), rs.getString("plan_name"),
                rs.getString("cycle_type"), (Long) rs.getObject("executor_user_id"),
                rs.getObject("start_date", LocalDate.class), rs.getObject("end_date", LocalDate.class),
                rs.getString("status"), rs.getTimestamp("created_at").toLocalDateTime());
    }

    private PatrolTaskView mapTask(ResultSet rs, int rowNum) throws SQLException {
        return new PatrolTaskView(rs.getLong("task_id"), rs.getLong("project_id"), (Long) rs.getObject("plan_id"),
                rs.getString("task_no"), rs.getString("task_name"), (Long) rs.getObject("executor_user_id"),
                rs.getTimestamp("planned_start_at").toLocalDateTime(), rs.getTimestamp("planned_end_at").toLocalDateTime(),
                localDateTime(rs, "actual_start_at"), localDateTime(rs, "actual_end_at"), rs.getString("status"),
                rs.getTimestamp("created_at").toLocalDateTime());
    }

    private PatrolTaskItemView mapTaskItem(ResultSet rs, int rowNum) throws SQLException {
        return new PatrolTaskItemView(rs.getLong("item_id"), rs.getLong("task_id"), rs.getLong("point_id"),
                rs.getString("result"), rs.getString("content"), rs.getString("image_file_ids"),
                localDateTime(rs, "checked_at"), rs.getString("status"), rs.getTimestamp("created_at").toLocalDateTime());
    }

    private LocalDateTime localDateTime(ResultSet rs, String column) throws SQLException {
        return rs.getTimestamp(column) == null ? null : rs.getTimestamp(column).toLocalDateTime();
    }

    private long value(Long value) {
        return value == null ? 0L : value;
    }
}
