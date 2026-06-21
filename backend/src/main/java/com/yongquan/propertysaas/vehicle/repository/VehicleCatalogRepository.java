package com.yongquan.propertysaas.vehicle.repository;

import com.yongquan.propertysaas.vehicle.domain.VehicleBrandView;
import com.yongquan.propertysaas.vehicle.domain.VehicleModelView;
import com.yongquan.propertysaas.vehicle.dto.VehicleBrandRequest;
import com.yongquan.propertysaas.vehicle.dto.VehicleModelRequest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class VehicleCatalogRepository {

    private final JdbcTemplate jdbcTemplate;

    public VehicleCatalogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<VehicleBrandView> findBrands(Long tenantId, String keyword, String status, long offset, long pageSize) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT brand_id, brand_name, brand_code, sort_no, status, created_at
                FROM base_vehicle_brand
                WHERE tenant_id = ? AND deleted = 0
                """);
        args.add(tenantId);
        appendKeyword(sql, args, "brand_name", keyword);
        appendStatus(sql, args, status);
        sql.append(" ORDER BY sort_no ASC, brand_name ASC LIMIT ? OFFSET ?");
        args.add(pageSize);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), this::mapBrand, args.toArray());
    }

    public long countBrands(Long tenantId, String keyword, String status) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM base_vehicle_brand WHERE tenant_id = ? AND deleted = 0");
        args.add(tenantId);
        appendKeyword(sql, args, "brand_name", keyword);
        appendStatus(sql, args, status);
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return count == null ? 0 : count;
    }

    public VehicleBrandView getBrand(Long tenantId, Long brandId) {
        return jdbcTemplate.queryForObject("""
                SELECT brand_id, brand_name, brand_code, sort_no, status, created_at
                FROM base_vehicle_brand
                WHERE tenant_id = ? AND brand_id = ? AND deleted = 0
                """, this::mapBrand, tenantId, brandId);
    }

    public void insertBrand(Long tenantId, Long brandId, Long userId, VehicleBrandRequest request) {
        jdbcTemplate.update("""
                        INSERT INTO base_vehicle_brand(brand_id, tenant_id, brand_name, brand_code, sort_no, status, created_by)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """, brandId, tenantId, trim(request.brandName()), trim(request.brandCode()),
                request.sortNo() == null ? 0 : request.sortNo(), text(request.status(), "ACTIVE"), userId);
    }

    public void updateBrand(Long tenantId, Long brandId, Long userId, VehicleBrandRequest request) {
        jdbcTemplate.update("""
                        UPDATE base_vehicle_brand
                        SET brand_name = ?, brand_code = ?, sort_no = ?, status = ?, updated_by = ?
                        WHERE tenant_id = ? AND brand_id = ? AND deleted = 0
                        """, trim(request.brandName()), trim(request.brandCode()),
                request.sortNo() == null ? 0 : request.sortNo(), text(request.status(), "ACTIVE"),
                userId, tenantId, brandId);
    }

    public boolean brandNameExists(Long tenantId, String brandName, Long excludeBrandId) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*) FROM base_vehicle_brand
                WHERE tenant_id = ? AND brand_name = ? AND deleted = 0
                """);
        args.add(tenantId);
        args.add(trim(brandName));
        if (excludeBrandId != null) {
            sql.append(" AND brand_id <> ?");
            args.add(excludeBrandId);
        }
        Integer count = jdbcTemplate.queryForObject(sql.toString(), Integer.class, args.toArray());
        return count != null && count > 0;
    }

    public boolean brandExists(Long tenantId, Long brandId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM base_vehicle_brand
                WHERE tenant_id = ? AND brand_id = ? AND deleted = 0 AND status = 'ACTIVE'
                """, Integer.class, tenantId, brandId);
        return count != null && count > 0;
    }

    public List<VehicleModelView> findModels(Long tenantId, Long brandId, String brandName,
                                             String keyword, String status, long offset, long pageSize) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT m.model_id, m.brand_id, b.brand_name, m.model_name, m.sort_no, m.status, m.created_at
                FROM base_vehicle_model m
                JOIN base_vehicle_brand b
                  ON b.tenant_id = m.tenant_id AND b.brand_id = m.brand_id AND b.deleted = 0
                WHERE m.tenant_id = ? AND m.deleted = 0
                """);
        args.add(tenantId);
        if (brandId != null) {
            sql.append(" AND m.brand_id = ?");
            args.add(brandId);
        }
        if (brandName != null && !brandName.isBlank()) {
            sql.append(" AND b.brand_name = ?");
            args.add(brandName.trim());
        }
        appendKeyword(sql, args, "m.model_name", keyword);
        appendStatus(sql, args, "m.status", status);
        sql.append(" ORDER BY b.sort_no ASC, b.brand_name ASC, m.sort_no ASC, m.model_name ASC LIMIT ? OFFSET ?");
        args.add(pageSize);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), this::mapModel, args.toArray());
    }

    public long countModels(Long tenantId, Long brandId, String brandName, String keyword, String status) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*)
                FROM base_vehicle_model m
                JOIN base_vehicle_brand b
                  ON b.tenant_id = m.tenant_id AND b.brand_id = m.brand_id AND b.deleted = 0
                WHERE m.tenant_id = ? AND m.deleted = 0
                """);
        args.add(tenantId);
        if (brandId != null) {
            sql.append(" AND m.brand_id = ?");
            args.add(brandId);
        }
        if (brandName != null && !brandName.isBlank()) {
            sql.append(" AND b.brand_name = ?");
            args.add(brandName.trim());
        }
        appendKeyword(sql, args, "m.model_name", keyword);
        appendStatus(sql, args, "m.status", status);
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return count == null ? 0 : count;
    }

    public VehicleModelView getModel(Long tenantId, Long modelId) {
        return jdbcTemplate.queryForObject("""
                SELECT m.model_id, m.brand_id, b.brand_name, m.model_name, m.sort_no, m.status, m.created_at
                FROM base_vehicle_model m
                JOIN base_vehicle_brand b
                  ON b.tenant_id = m.tenant_id AND b.brand_id = m.brand_id AND b.deleted = 0
                WHERE m.tenant_id = ? AND m.model_id = ? AND m.deleted = 0
                """, this::mapModel, tenantId, modelId);
    }

    public void insertModel(Long tenantId, Long modelId, Long userId, VehicleModelRequest request) {
        jdbcTemplate.update("""
                        INSERT INTO base_vehicle_model(model_id, tenant_id, brand_id, model_name, sort_no, status, created_by)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """, modelId, tenantId, request.brandId(), trim(request.modelName()),
                request.sortNo() == null ? 0 : request.sortNo(), text(request.status(), "ACTIVE"), userId);
    }

    public void updateModel(Long tenantId, Long modelId, Long userId, VehicleModelRequest request) {
        jdbcTemplate.update("""
                        UPDATE base_vehicle_model
                        SET brand_id = ?, model_name = ?, sort_no = ?, status = ?, updated_by = ?
                        WHERE tenant_id = ? AND model_id = ? AND deleted = 0
                        """, request.brandId(), trim(request.modelName()), request.sortNo() == null ? 0 : request.sortNo(),
                text(request.status(), "ACTIVE"), userId, tenantId, modelId);
    }

    public boolean modelNameExists(Long tenantId, Long brandId, String modelName, Long excludeModelId) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*) FROM base_vehicle_model
                WHERE tenant_id = ? AND brand_id = ? AND model_name = ? AND deleted = 0
                """);
        args.add(tenantId);
        args.add(brandId);
        args.add(trim(modelName));
        if (excludeModelId != null) {
            sql.append(" AND model_id <> ?");
            args.add(excludeModelId);
        }
        Integer count = jdbcTemplate.queryForObject(sql.toString(), Integer.class, args.toArray());
        return count != null && count > 0;
    }

    private void appendKeyword(StringBuilder sql, List<Object> args, String field, String keyword) {
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND ").append(field).append(" LIKE ?");
            args.add("%" + keyword.trim() + "%");
        }
    }

    private void appendStatus(StringBuilder sql, List<Object> args, String status) {
        appendStatus(sql, args, "status", status);
    }

    private void appendStatus(StringBuilder sql, List<Object> args, String field, String status) {
        if (status != null && !status.isBlank()) {
            sql.append(" AND ").append(field).append(" = ?");
            args.add(status);
        }
    }

    private VehicleBrandView mapBrand(ResultSet rs, int rowNum) throws SQLException {
        return new VehicleBrandView(rs.getLong("brand_id"), rs.getString("brand_name"),
                rs.getString("brand_code"), rs.getInt("sort_no"), rs.getString("status"),
                rs.getTimestamp("created_at").toLocalDateTime());
    }

    private VehicleModelView mapModel(ResultSet rs, int rowNum) throws SQLException {
        return new VehicleModelView(rs.getLong("model_id"), rs.getLong("brand_id"), rs.getString("brand_name"),
                rs.getString("model_name"), rs.getInt("sort_no"), rs.getString("status"),
                rs.getTimestamp("created_at").toLocalDateTime());
    }

    private String text(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
