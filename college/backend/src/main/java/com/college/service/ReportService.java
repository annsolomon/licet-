package com.college.service;

import com.college.dto.AttendanceSummary;
import com.college.dto.ReportRequest;
import com.college.exception.ValidationException;
import com.college.model.Department;
import com.college.model.MarkEntry;
import com.college.model.PayrollRecord;
import com.college.model.ResultRow;
import com.college.model.Student;
import com.college.repository.MarksRepository;
import com.college.repository.ReportRepository;
import com.college.util.FileUtil;
import com.college.util.GenericAverage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * REPORTS = SYLLABUS: file operations + multithreading + interfaces + generics + collections, all in one service.
 *
 *   React "Generate" --> ReportController --> ReportService.generate()
 *      --> ReportGenerator (interface; one implementation per report type) reads the data through other services
 *      --> FileUtil.writeLines()  writes  output/STUDENT_20261008_183000.txt   (file handling)
 *      --> preview + character count returned as JSON
 *
 *   generateParallel(): ExecutorService runs several generators AT THE SAME TIME (multithreading).
 */
@Service
public class ReportService {

    /** Result of generating one report file. */
    public record ReportFile(String name, String type, long characters, int lines, String thread,
                             long millis, List<String> preview) { }

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final Map<String, ReportGenerator> generators = new LinkedHashMap<>();   // collection: type -> generator
    private final StudentService students;
    private final AttendanceService attendance;
    private final ResultService results;
    private final PayrollService payroll;
    private final DepartmentService departments;
    private final MarksRepository marksRepository;
    private final ReportRepository reportRepository;
    private final Path reportsDir;

    public ReportService(StudentService students, AttendanceService attendance, ResultService results,
                         PayrollService payroll, DepartmentService departments,
                         MarksRepository marksRepository, ReportRepository reportRepository,
                         @Value("${app.reports.dir:../output}") String reportsDir) {
        this.students = students;
        this.attendance = attendance;
        this.results = results;
        this.payroll = payroll;
        this.departments = departments;
        this.marksRepository = marksRepository;
        this.reportRepository = reportRepository;
        this.reportsDir = Path.of(reportsDir).toAbsolutePath().normalize();
        register("STUDENT", this::studentReport);
        register("ATTENDANCE", this::attendanceReport);
        register("MARKS", this::marksReport);
        register("RESULT", this::resultReport);
        register("DEPARTMENT", this::departmentReport);
        register("PAYROLL", this::payrollReport);
        register("LOW_ATTENDANCE", this::lowAttendanceReport);
        register("TOP_PERFORMERS", this::topPerformersReport);
    }

    private void register(String type, Function<ReportRequest, List<String>> body) {
        generators.put(type, new ReportGenerator() {            // anonymous class implementing the interface
            @Override public String type() { return type; }
            @Override public List<String> generate(ReportRequest request) { return body.apply(request); }
        });
    }

    public List<String> types() {
        return new ArrayList<>(generators.keySet());
    }

    // ------------------------------------------------------------------ generate
    public ReportFile generate(ReportRequest request) {
        String type = request == null || request.type() == null ? "" : request.type().trim().toUpperCase();
        ReportGenerator generator = generators.get(type);
        if (generator == null) {
            throw new ValidationException("Unknown report type '" + type + "'. Use one of " + types());
        }
        long start = System.currentTimeMillis();
        List<String> lines = new ArrayList<>();
        lines.add("COLLEGE ACADEMIC MANAGEMENT SYSTEM - " + type.replace('_', ' ') + " REPORT");
        lines.add("Generated: " + LocalDateTime.now().withNano(0));
        lines.add("-".repeat(78));
        lines.addAll(generator.generate(request));

        String fileName = type + "_" + LocalDateTime.now().format(STAMP) + ".txt";
        long chars;
        try {
            chars = FileUtil.writeLines(reportsDir.resolve(fileName), lines);
        } catch (IOException e) {
            throw new ValidationException("Could not write report file: " + e.getMessage());
        }
        return new ReportFile(fileName, type, chars, lines.size(), Thread.currentThread().getName(),
                System.currentTimeMillis() - start, lines.subList(0, Math.min(lines.size(), 25)));
    }

    /**
     * MULTITHREADING: every type is generated by its own worker thread of a fixed pool.
     * Future = "a promise of a result"; future.get() waits until that task has finished.
     */
    public List<ReportFile> generateParallel(List<String> typeNames) {
        if (typeNames == null || typeNames.isEmpty()) {
            throw new ValidationException("Choose at least one report type");
        }
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(4, typeNames.size()));
        try {
            List<Future<ReportFile>> futures = new ArrayList<>();
            for (String type : typeNames) {
                futures.add(pool.submit(() -> generate(new ReportRequest(type, null, null, null))));
            }
            List<ReportFile> files = new ArrayList<>();
            for (Future<ReportFile> f : futures) {
                try {
                    files.add(f.get());
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    throw cause instanceof RuntimeException re ? re : new ValidationException(cause.getMessage());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new ValidationException("Report generation was interrupted");
                }
            }
            return files;
        } finally {
            pool.shutdown();
        }
    }

    // ------------------------------------------------------------------ file operations on exported reports
    public List<Map<String, Object>> listFiles() {
        List<Map<String, Object>> list = new ArrayList<>();
        if (!Files.isDirectory(reportsDir)) {
            return list;
        }
        try (Stream<Path> stream = Files.list(reportsDir)) {
            stream.filter(p -> p.getFileName().toString().endsWith(".txt"))
                    .sorted((a, b) -> b.getFileName().toString().compareTo(a.getFileName().toString()))
                    .forEach(p -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("name", p.getFileName().toString());
                        try {
                            m.put("characters", FileUtil.countCharacters(p));
                        } catch (IOException e) {
                            m.put("characters", -1);
                        }
                        list.add(m);
                    });
        } catch (IOException e) {
            throw new ValidationException("Cannot list reports: " + e.getMessage());
        }
        return list;
    }

    public List<String> read(String name) {
        try {
            return FileUtil.readLines(resolve(name));
        } catch (IOException e) {
            throw new ValidationException("File not found: " + name);
        }
    }

    /** SYLLABUS: count character occurrences in a file. */
    public Map<String, Object> countCharacter(String name, String character) {
        if (character == null || character.length() != 1) {
            throw new ValidationException("Give exactly one character to count");
        }
        try {
            Path file = resolve(name);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("file", name);
            m.put("character", character);
            m.put("occurrences", FileUtil.countOccurrences(file, character.charAt(0)));
            m.put("totalCharacters", FileUtil.countCharacters(file));
            return m;
        } catch (IOException e) {
            throw new ValidationException("File not found: " + name);
        }
    }

    /** SYLLABUS: copy one file to another. */
    public Map<String, Object> copy(String name) {
        try {
            Path source = resolve(name);
            String copyName = "COPY_" + name;
            long copied = FileUtil.copyFile(source, reportsDir.resolve(copyName));
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("source", name);
            m.put("copy", copyName);
            m.put("charactersCopied", copied);
            return m;
        } catch (IOException e) {
            throw new ValidationException("File not found: " + name);
        }
    }

    public Path fileForDownload(String name) {
        Path p = resolve(name);
        if (!Files.exists(p)) {
            throw new ValidationException("File not found: " + name);
        }
        return p;
    }

    /** Only simple names inside the reports folder are allowed (blocks "../../etc/passwd"). */
    private Path resolve(String name) {
        if (name == null || !name.matches("[A-Za-z0-9_.-]+\\.txt")) {
            throw new ValidationException("Invalid file name");
        }
        Path p = reportsDir.resolve(name).normalize();
        if (!p.startsWith(reportsDir)) {
            throw new ValidationException("Invalid file name");
        }
        return p;
    }

    // ------------------------------------------------------------------ the eight reports
    private List<String> studentReport(ReportRequest r) {
        List<Student> list = r.departmentId() == null ? students.findAll() : students.findByDepartment(r.departmentId());
        List<String> out = new ArrayList<>();
        out.add(String.format("%-6s %-12s %-6s %-12s %s", "ID", "NAME", "DEPT", "CLASS", "E-MAIL"));
        for (Student s : list) {
            out.add(String.format("%-6d %-12s %-6s %-12s %s", s.getId(), s.getName(), s.getDeptCode(), s.getClassName(), s.getEmail()));
        }
        out.add("Total students: " + list.size());
        return out;
    }

    private List<String> attendanceReport(ReportRequest r) {
        List<AttendanceSummary> list = r.departmentId() == null ? attendance.all() : attendance.byDepartment(r.departmentId());
        List<String> out = new ArrayList<>();
        out.add(String.format("%-6s %-12s %8s %8s %10s", "ID", "NAME", "PRESENT", "TOTAL", "ATT %"));
        List<BigDecimal> percentages = new ArrayList<>();
        for (AttendanceSummary a : list) {
            out.add(String.format("%-6d %-12s %8d %8d %9s%s", a.studentId(), a.studentName(), a.present(), a.total(),
                    a.percentage(), a.low() ? "  LOW" : ""));
            percentages.add(a.percentage());
        }
        out.add(String.format("Average attendance: %.2f %%  (GenericAverage<BigDecimal>)", new GenericAverage<>(percentages).average()));
        return out;
    }

    private List<String> marksReport(ReportRequest r) {
        List<Student> list = r.departmentId() == null ? students.findAll() : students.findByDepartment(r.departmentId());
        List<String> out = new ArrayList<>();
        out.add("RAW marks only (never the converted values)");
        for (Student s : list) {
            Map<String, List<MarkEntry>> bySubject = new LinkedHashMap<>();
            for (MarkEntry m : marksRepository.findMarks(s.getId(), null)) {
                bySubject.computeIfAbsent(m.subjectCode(), k -> new ArrayList<>()).add(m);
            }
            for (Map.Entry<String, List<MarkEntry>> e : bySubject.entrySet()) {
                StringBuilder sb = new StringBuilder(String.format("%-5d %-10s %-6s", s.getId(), s.getName(), e.getKey()));
                for (MarkEntry m : e.getValue()) {
                    sb.append(' ').append(m.assessmentCode()).append('=').append(m.rawMarks().stripTrailingZeros().toPlainString());
                }
                out.add(sb.toString());
            }
        }
        return out;
    }

    private List<String> resultReport(ReportRequest r) {
        List<ResultRow> rows = results.list(null, r.departmentId());
        List<String> out = new ArrayList<>();
        out.add(String.format("%-5s %-10s %-6s %-3s %9s %9s %8s %-5s %-3s %-4s", "ID", "NAME", "SUBJ", "SEM",
                "INT/40", "SEM/60", "FINAL", "GRADE", "GP", "P/F"));
        List<BigDecimal> finals = new ArrayList<>();
        for (ResultRow x : rows) {
            out.add(String.format("%-5d %-10s %-6s %-3d %9s %9s %8s %-5s %-3s %-4s", x.studentId(), x.studentName(),
                    x.subjectCode(), x.semester(), x.internalMark(), x.semesterMark(), x.finalMark(), x.grade(),
                    x.gradePoint().stripTrailingZeros().toPlainString(), x.passFail()));
            finals.add(x.finalMark());
        }
        out.add(String.format("Average final mark: %.2f", new GenericAverage<>(finals).average()));
        return out;
    }

    /** Uses the Oracle procedure department_report_text (explicit cursor) for every department. */
    private List<String> departmentReport(ReportRequest r) {
        List<String> out = new ArrayList<>();
        List<Department> list = r.departmentId() == null ? departments.findAll() : List.of(departments.get(r.departmentId()));
        for (Department d : list) {
            out.addAll(List.of(reportRepository.departmentReport(d.id()).split("\n")));
            out.add("");
        }
        return out;
    }

    private List<String> payrollReport(ReportRequest r) {
        List<PayrollRecord> rows = payroll.list(r.month());
        List<String> out = new ArrayList<>();
        out.add(String.format("%-6s %-18s %-20s %-8s %11s %11s %11s", "EMP", "NAME", "DESIGNATION", "MONTH", "BASIC", "GROSS", "NET"));
        BigDecimal totalNet = BigDecimal.ZERO;
        List<BigDecimal> nets = new ArrayList<>();
        for (PayrollRecord p : rows) {
            out.add(String.format("%-6d %-18s %-20s %-8s %11s %11s %11s", p.empId(), p.empName(), p.designation(),
                    p.month(), p.basic(), p.gross(), p.net()));
            totalNet = totalNet.add(p.net());
            nets.add(p.net());
        }
        out.add("Total net payroll: Rs. " + totalNet.toPlainString());
        out.add(String.format("Average net salary: Rs. %.2f", new GenericAverage<>(nets).average()));
        return out;
    }

    private List<String> lowAttendanceReport(ReportRequest r) {
        List<AttendanceSummary> low = attendance.low(r.threshold());
        List<String> out = new ArrayList<>();
        out.add("Students below " + (r.threshold() == null ? attendance.threshold() : r.threshold()) + " % attendance");
        out.add(String.format("%-6s %-12s %8s %8s %9s", "ID", "NAME", "PRESENT", "TOTAL", "ATT %"));
        for (AttendanceSummary a : low) {
            out.add(String.format("%-6d %-12s %8d %8d %9s", a.studentId(), a.studentName(), a.present(), a.total(), a.percentage()));
        }
        out.add("Students listed: " + low.size());
        return out;
    }

    private List<String> topPerformersReport(ReportRequest r) {
        List<Map<String, Object>> top = results.topPerformers(10);
        List<String> out = new ArrayList<>();
        out.add(String.format("%-4s %-6s %-12s %-6s %6s", "RANK", "ID", "NAME", "DEPT", "CGPA"));
        int rank = 0;
        for (Map<String, Object> m : top) {
            rank++;
            out.add(String.format("%-4d %-6s %-12s %-6s %6s", rank, m.get("studentId"), m.get("studentName"), m.get("deptCode"), m.get("cgpa")));
        }
        return out;
    }
}
