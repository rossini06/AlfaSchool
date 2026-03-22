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
        String tid = tenantId.toString();

        long totalStudents = count(
            "SELECT COUNT(*) FROM alunos WHERE tenant_id = ? AND deleted = false AND ativo = true", tid);

        long totalStaff = count(
            "SELECT COUNT(*) FROM professores WHERE tenant_id = ? AND deleted = false", tid);

        long attendanceToday = count(
            "SELECT COUNT(*) FROM frequencias WHERE tenant_id = ? AND data = CURRENT_DATE AND presente = true AND deleted = false", tid);

        long overduePayments = count(
            "SELECT COUNT(*) FROM cobrancas WHERE tenant_id = ? AND vencimento < CURRENT_DATE AND status = 'pendente' AND deleted = false", tid);

        return new DashboardSnapshot(totalStudents, totalStaff, attendanceToday, overduePayments, 0L);
    }

    private long count(String sql, String tenantId) {
        try {
            Long value = jdbcTemplate.queryForObject(sql, Long.class, tenantId);
            return value == null ? 0L : value;
        } catch (DataAccessException e) {
            return 0L;
        }
    }
}
