package com.college.portals.parent;

import com.college.portals.common.ApiException;
import com.college.portals.common.AuthInterceptor;
import com.college.portals.common.Dates;
import com.college.portals.common.Session;
import com.college.portals.common.StudentViewRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * PARENT PORTAL (port 8083).  A parent sees only his or her own child.
 *   GET /api/ward                      child + overall attendance + advisor contact
 *   GET /api/subjects                  subject-wise attendance (with "can miss" / "must attend" counts)
 *   GET /api/day?date=2026-10-08       the 8 periods of a day, each Present / Absent
 *   GET /api/history?days=14           day by day, with the absent periods
 *   GET /api/internals                 internal marks (ASG, CT, CAT) and the internal mark /40
 *   GET /api/notifications ...         absence alerts (InboxController, parent only)
 * The STUDENT portal (port 8086) is this same controller: a student sees only his or her own data.
 */
@RestController
@RequestMapping("/api")
@Profile({"parent", "student"})
public class ParentController {

    private static final int THRESHOLD = 75;
    private final ParentRepository repository;
    private final StudentViewRepository views;

    public ParentController(ParentRepository repository, StudentViewRepository views) {
        this.repository = repository;
        this.views = views;
    }

    private long wardId(HttpServletRequest request, Long studentId) {
        Session s = AuthInterceptor.session(request);
        if ("STUDENT".equals(s.role())) {      // a student sees only himself / herself
            return s.linkId();
        }
        if (studentId != null) {
            if (!repository.isWardOf(s.linkId(), studentId)) {
                throw ApiException.forbidden("This student is not your ward");
            }
            return studentId;
        }
        List<Map<String, Object>> wards = repository.wards(s.role(), s.linkId());
        if (wards.isEmpty()) {
            throw ApiException.notFound("No student is linked to this account");
        }
        return ((Number) wards.get(0).get("id")).longValue();
    }

    @GetMapping("/ward")
    public Map<String, Object> ward(HttpServletRequest request, @RequestParam(required = false) Long studentId) {
        long id = wardId(request, studentId);
        Session s = AuthInterceptor.session(request);
        Map<String, Object> out = new LinkedHashMap<>(repository.wards(s.role(), s.linkId()).stream()
                .filter(w -> ((Number) w.get("id")).longValue() == id).findFirst().orElseThrow());
        out.put("attendance", repository.overall(id));
        out.put("threshold", THRESHOLD);
        out.put("latestDate", repository.latestDate(id));
        return out;
    }

    @GetMapping("/subjects")
    public List<Map<String, Object>> subjects(HttpServletRequest request, @RequestParam(required = false) Long studentId) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map<String, Object> row : repository.subjects(wardId(request, studentId))) {
            Map<String, Object> m = new LinkedHashMap<>(row);
            long present = ((Number) row.get("present")).longValue();
            long total = ((Number) row.get("total")).longValue();
            BigDecimal pct = (BigDecimal) row.get("percentage");
            boolean low = pct != null && pct.compareTo(BigDecimal.valueOf(THRESHOLD)) < 0;
            m.put("low", low);
            // attend x more periods in a row to reach 75 %:  (present + x) / (total + x) >= 0.75  ->  x >= 3 total - 4 present
            m.put("mustAttend", low ? Math.max(0, 3 * total - 4 * present) : 0);
            // can miss y more periods and still stay at 75 %:  present / (total + y) >= 0.75  ->  y <= (4 present - 3 total) / 3
            m.put("canMiss", low ? 0 : Math.max(0, Math.floorDiv(4 * present - 3 * total, 3)));
            rows.add(m);
        }
        return rows;
    }

    @GetMapping("/day")
    public Map<String, Object> day(HttpServletRequest request, @RequestParam(required = false) String date,
                                   @RequestParam(required = false) Long studentId) {
        long id = wardId(request, studentId);
        String d = date == null || date.isBlank() ? repository.latestDate(id) : Dates.require(date);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("date", d);
        out.put("periods", repository.day(id, d));
        return out;
    }

    @GetMapping("/history")
    public List<Map<String, Object>> history(HttpServletRequest request, @RequestParam(defaultValue = "14") int days,
                                             @RequestParam(required = false) Long studentId) {
        return repository.history(wardId(request, studentId), Math.max(1, Math.min(days, 90)));
    }

    /** internal-assessment marks of the ward (or of the student himself): ASG / CT / CAT, internal mark out of 40 */
    @GetMapping("/internals")
    public Map<String, Object> internals(HttpServletRequest request, @RequestParam(required = false) Long studentId) {
        return views.internals(wardId(request, studentId));
    }
}
