package com.college.service;

import com.college.dto.AttendanceSummary;
import com.college.dto.DashboardStats;
import com.college.repository.DashboardRepository;
import com.college.repository.ResultRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Numbers for the dashboard cards and charts (COUNT queries + a few existing services). */
@Service
public class DashboardService {

    private final DashboardRepository repository;
    private final ResultRepository results;
    private final AttendanceService attendance;

    public DashboardService(DashboardRepository repository, ResultRepository results, AttendanceService attendance) {
        this.repository = repository;
        this.results = results;
        this.attendance = attendance;
    }

    public DashboardStats stats() {
        List<AttendanceSummary> all = attendance.all();
        BigDecimal avg = BigDecimal.ZERO;
        if (!all.isEmpty()) {
            BigDecimal sum = BigDecimal.ZERO;
            int counted = 0;
            for (AttendanceSummary a : all) {
                if (a.percentage() != null) {
                    sum = sum.add(a.percentage());
                    counted++;
                }
            }
            avg = counted == 0 ? BigDecimal.ZERO : sum.divide(BigDecimal.valueOf(counted), 2, RoundingMode.HALF_UP);
        }
        long low = all.stream().filter(AttendanceSummary::low).count();
        long pass = results.countByPassFail("PASS");
        long fail = results.countByPassFail("FAIL");
        BigDecimal passPct = pass + fail == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(pass * 100.0 / (pass + fail)).setScale(1, RoundingMode.HALF_UP);
        return new DashboardStats(
                repository.count("student"), repository.count("faculty"), repository.count("subject"),
                repository.count("department"), repository.count("class"), repository.count("employee"),
                avg, low, pass, fail, passPct,
                repository.studentsByDepartment(), results.gradeDistribution(),
                List.of("React Dashboard", "GET /api/dashboard", "DashboardController", "DashboardService",
                        "DashboardRepository / ResultRepository", "JdbcTemplate", "SELECT COUNT(*) ...", "Oracle"));
    }
}
