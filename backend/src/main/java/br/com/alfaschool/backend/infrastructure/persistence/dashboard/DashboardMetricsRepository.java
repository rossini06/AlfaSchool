package br.com.alfaschool.backend.infrastructure.persistence.dashboard;

import br.com.alfaschool.backend.domain.dashboard.DashboardSnapshot;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class DashboardMetricsRepository {

    private final JdbcTemplate jdbcTemplate;

    public DashboardMetricsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public DashboardSnapshot buildSnapshot(UUID tenantId, UUID unitId) {
        return new DashboardSnapshot(
                queryCount("SELECT COUNT(*) FROM students WHERE tenant_id = ?" + unitCondition("unit_id", unitId), tenantId, unitId),
                queryCount("SELECT COUNT(*) FROM users WHERE tenant_id = ?" + unitCondition("unit_id", unitId), tenantId, unitId),
                queryCount("SELECT COUNT(*) FROM attendances WHERE tenant_id = ? AND attendance_date = CURRENT_DATE" + unitCondition("unit_id", unitId), tenantId, unitId),
                queryCount("SELECT COUNT(*) FROM payments WHERE tenant_id = ? AND due_date < CURRENT_DATE AND paid = false" + unitCondition("unit_id", unitId), tenantId, unitId),
                queryCount("SELECT COUNT(*) FROM access_logs WHERE tenant_id = ? AND DATE(access_time) = CURRENT_DATE" + unitCondition("unit_id", unitId), tenantId, unitId)
        );
    }

    private long queryCount(String sql, UUID tenantId, UUID unitId) {
        try {
            if (unitId == null) {
                Long value = jdbcTemplate.queryForObject(sql, Long.class, tenantId);
                return value == null ? 0L : value;
            }
            Long value = jdbcTemplate.queryForObject(sql, Long.class, tenantId, unitId);
            return value == null ? 0L : value;
        } catch (DataAccessException exception) {
            return 0L;
        }
    }

    private String unitCondition(String column, UUID unitId) {
        return unitId == null ? "" : " AND " + column + " = ?";
    }
}
