package com.college.portals.advisor;

import com.college.portals.common.ApiException;
import com.college.portals.common.AuthInterceptor;
import com.college.portals.common.Dates;
import com.college.portals.common.StudentViewRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CLASS ADVISOR PORTAL (port 8084).
 *   GET  /api/class                        my class
 *   GET  /api/day?date=                    8-period grid: every student x every period (+ the timetable of the day)
 *   PUT  /api/records/{id}                 correct one cell  {"status":"ABSENT"}   -> Oracle trigger notifies
 *   POST /api/period/mark                  mark a whole period {"date","period","absentIds":[...]}
 *   GET  /api/students?date=               students with overall % and today's absences + parent phone
 *   GET  /api/subjects                     subject-wise attendance of the class
 *   GET  /api/low?threshold=75             students below the limit
 *   GET  /api/notifications ...            the advisor's alerts (InboxController)
 */
@RestController
@RequestMapping("/api")
@Profile("advisor")
public class AdvisorController {

    public record StatusRequest(String status) { }

    public record PeriodRequest(String date, Integer period, List<Long> absentIds) { }

    private final AdvisorRepository repository;
    private final StudentViewRepository views;

    public AdvisorController(AdvisorRepository repository, StudentViewRepository views) {
        this.repository = repository;
        this.views = views;
    }

    private long classId(HttpServletRequest request) {
        return repository.classOf(AuthInterceptor.session(request).linkId());
    }

    private String dateOrLatest(long classId, String date) {
        if (date != null && !date.isBlank()) {
            return Dates.require(date);
        }
        return (String) repository.classInfo(classId).get("latestDate");
    }

    @GetMapping("/class")
    public Map<String, Object> myClass(HttpServletRequest request) {
        return repository.classInfo(classId(request));
    }

    @GetMapping("/day")
    public Map<String, Object> day(HttpServletRequest request, @RequestParam(required = false) String date) {
        long classId = classId(request);
        String d = dateOrLatest(classId, date);
        Map<String, Map<String, Object>> students = new LinkedHashMap<>();
        for (AdvisorRepository.Cell c : repository.cells(classId, d)) {
            Map<String, Object> s = students.computeIfAbsent(String.valueOf(c.studentId()), k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", c.studentId());
                m.put("name", c.studentName());
                m.put("absent", 0);
                m.put("cells", new ArrayList<Map<String, Object>>());
                return m;
            });
            Map<String, Object> cell = new LinkedHashMap<>();
            cell.put("period", c.period());
            cell.put("recordId", c.recordId());
            cell.put("status", c.status());
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> cells = (List<Map<String, Object>>) s.get("cells");
            cells.add(cell);
            if ("ABSENT".equals(c.status())) {
                s.put("absent", (Integer) s.get("absent") + 1);
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("date", d);
        out.put("periods", repository.periods(classId, d));
        out.put("students", new ArrayList<>(students.values()));
        return out;
    }

    @PutMapping("/records/{id}")
    public Map<String, Object> setStatus(HttpServletRequest request, @PathVariable long id, @RequestBody StatusRequest body) {
        repository.setStatus(classId(request), id, body.status());
        return Map.of("recordId", id, "status", body.status().trim().toUpperCase());
    }

    @PostMapping("/period/mark")
    public Map<String, Object> markPeriod(HttpServletRequest request, @RequestBody PeriodRequest body) {
        if (body.period() == null) {
            throw ApiException.badRequest("period is required");
        }
        return repository.markPeriod(classId(request), Dates.require(body.date()), body.period(), body.absentIds());
    }

    @GetMapping("/students")
    public List<Map<String, Object>> students(HttpServletRequest request, @RequestParam(required = false) String date) {
        long classId = classId(request);
        return repository.students(classId, dateOrLatest(classId, date));
    }

    @GetMapping("/subjects")
    public List<Map<String, Object>> subjects(HttpServletRequest request) {
        return repository.subjects(classId(request));
    }

    @GetMapping("/low")
    public List<Map<String, Object>> low(HttpServletRequest request, @RequestParam(defaultValue = "75") int threshold) {
        return repository.low(classId(request), threshold);
    }

    /** every student of the class with attendance and internal average */
    @GetMapping("/overview")
    public List<Map<String, Object>> overview(HttpServletRequest request) {
        return views.overview("s.class_id", classId(request));
    }

    @GetMapping("/internals/subjects")
    public List<Map<String, Object>> internalsBySubject(HttpServletRequest request) {
        return views.subjectSummary("s.class_id", classId(request));
    }

    /** students x subjects grid of internal marks /40 */
    @GetMapping("/internals/grid")
    public Map<String, Object> internalsGrid(HttpServletRequest request) {
        Map<String, Map<String, Object>> students = new LinkedHashMap<>();
        List<String> codes = new ArrayList<>();
        for (Map<String, Object> r : views.grid(classId(request))) {
            String code = (String) r.get("code");
            if (!codes.contains(code)) {
                codes.add(code);
            }
            Map<String, Object> s = students.computeIfAbsent(String.valueOf(r.get("id")), k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", r.get("id"));
                m.put("name", r.get("name"));
                m.put("marks", new LinkedHashMap<String, Object>());
                return m;
            });
            @SuppressWarnings("unchecked")
            Map<String, Object> marks = (Map<String, Object>) s.get("marks");
            marks.put(code, r.get("internal"));
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("passMark", StudentViewRepository.PASS_MARK);
        out.put("subjects", codes);
        out.put("students", new ArrayList<>(students.values()));
        return out;
    }

    @GetMapping("/student/{id}")
    public Map<String, Object> student(HttpServletRequest request, @PathVariable long id) {
        if (((Number) views.scope(id).get("classId")).longValue() != classId(request)) {
            throw ApiException.forbidden("That student is not in your class");
        }
        return views.detail(id);
    }
}
