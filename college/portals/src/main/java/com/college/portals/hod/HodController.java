package com.college.portals.hod;

import com.college.portals.common.AuthInterceptor;
import com.college.portals.common.ApiException;
import com.college.portals.common.Dates;
import com.college.portals.common.StudentViewRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * HOD PORTAL (port 8082) - the head of a department sees the whole department.
 *   GET /api/overview?date=      department, day statistics, period-wise %, 10-day trend, low-attendance count
 *   GET /api/classes             attendance of every class + advisor
 *   GET /api/subjects            subject-wise attendance with the teacher
 *   GET /api/faculty             teachers: periods taken, attendance in their classes
 *   GET /api/low?threshold=75    students below the limit (with parent phone)
 *   GET /api/absentees?date=     who was absent on a day and in which periods
 *   GET /api/notifications/summary   how many alerts were created / sent / failed
 */
@RestController
@RequestMapping("/api")
@Profile("hod")
public class HodController {

    private final HodRepository repository;
    private final StudentViewRepository views;

    public HodController(HodRepository repository, StudentViewRepository views) {
        this.repository = repository;
        this.views = views;
    }

    private long dept(HttpServletRequest request) {
        return repository.deptOf(AuthInterceptor.session(request).linkId());
    }

    private String dateOrLatest(long dept, String date) {
        return date == null || date.isBlank() ? (String) repository.department(dept).get("latestDate") : Dates.require(date);
    }

    @GetMapping("/overview")
    public Map<String, Object> overview(HttpServletRequest request, @RequestParam(required = false) String date) {
        long dept = dept(request);
        String d = dateOrLatest(dept, date);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("department", repository.department(dept));
        out.put("date", d);
        out.put("day", repository.dayStats(dept, d));
        out.put("periods", repository.periodWise(dept, d));
        out.put("trend", repository.trend(dept, 14));
        out.put("lowCount", repository.low(dept, 75).size());
        return out;
    }

    @GetMapping("/classes")
    public List<Map<String, Object>> classes(HttpServletRequest request) {
        return repository.classes(dept(request));
    }

    @GetMapping("/subjects")
    public List<Map<String, Object>> subjects(HttpServletRequest request) {
        return repository.subjects(dept(request));
    }

    @GetMapping("/faculty")
    public List<Map<String, Object>> faculty(HttpServletRequest request) {
        return repository.faculty(dept(request));
    }

    @GetMapping("/low")
    public List<Map<String, Object>> low(HttpServletRequest request, @RequestParam(defaultValue = "75") int threshold) {
        return repository.low(dept(request), threshold);
    }

    @GetMapping("/absentees")
    public Map<String, Object> absentees(HttpServletRequest request, @RequestParam(required = false) String date) {
        long dept = dept(request);
        String d = dateOrLatest(dept, date);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("date", d);
        out.put("students", repository.absentees(dept, d));
        return out;
    }

    @GetMapping("/notifications/summary")
    public List<Map<String, Object>> notifications(HttpServletRequest request) {
        return repository.notificationSummary(dept(request));
    }

    @GetMapping("/students")
    public List<Map<String, Object>> students(HttpServletRequest request) {
        return views.overview("s.dept_id", dept(request));
    }

    @GetMapping("/internals/subjects")
    public List<Map<String, Object>> internalsBySubject(HttpServletRequest request) {
        return views.subjectSummary("s.dept_id", dept(request));
    }

    @GetMapping("/student/{id}")
    public Map<String, Object> student(HttpServletRequest request, @PathVariable long id) {
        long dept = dept(request);
        if (((Number) views.scope(id).get("deptId")).longValue() != dept) {
            throw ApiException.forbidden("That student is not in your department");
        }
        return views.detail(id);
    }
}
