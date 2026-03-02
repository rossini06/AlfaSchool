package br.com.alfaschool.backend.domain.dashboard;

public record DashboardSnapshot(
        long totalStudents,
        long totalStaff,
        long attendanceToday,
        long overduePayments,
        long accessToday
) {
}
