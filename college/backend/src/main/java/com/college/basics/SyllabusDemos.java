package com.college.basics;

import com.college.employee.AssistantProfessor;
import com.college.employee.AssociateProfessor;
import com.college.employee.Employee;
import com.college.employee.EmployeeFactory;
import com.college.employee.Payable;
import com.college.employee.Professor;
import com.college.employee.Programmer;
import com.college.exception.InvalidMarksException;
import com.college.marks.MarkValidator;
import com.college.model.Faculty;
import com.college.model.Person;
import com.college.model.Student;
import com.college.payroll.PayrollCalculator;
import com.college.payroll.PayslipFormatter;
import com.college.util.FileUtil;
import com.college.util.GenericAverage;
import com.college.util.GenericSearch;
import com.college.util.Pair;
import com.college.util.Searchable;
import com.college.util.StringUtil;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * LEVEL 1 of every Java syllabus topic: a SMALL, RUNNABLE example with printed output.
 *
 *   Run all of them in the terminal:   ./run.sh syllabus
 *   See them in the browser:           "Java Lab" page (REST /api/lab/...)
 *
 * LEVEL 2 (the topic inside the real project) is named in the "where used" line of each demo.
 * Every method returns the output lines instead of printing, so the same code feeds the terminal and the web page.
 */
public final class SyllabusDemos {

    private SyllabusDemos() { }

    // ====================================================================== 1. SIMPLE PROGRAMS
    public static List<String> factorial() {
        List<String> out = new ArrayList<>();
        out.add("Input : 5");
        out.add("Output: " + BasicPrograms.factorial(5));
        out.add("Recursive version: " + BasicPrograms.factorialRecursive(5));
        out.add("factorial(0) = " + BasicPrograms.factorial(0));
        try {
            BasicPrograms.factorial(-3);
        } catch (IllegalArgumentException e) {
            out.add("factorial(-3) -> exception: " + e.getMessage());
        }
        return out;
    }

    public static List<String> fibonacci() {
        return List.of("Input : 8 terms", "Output: " + BasicPrograms.fibonacci(8));
    }

    public static List<String> binarySearch() {
        int[] data = {10, 20, 30, 40, 50};
        int found = BasicPrograms.binarySearch(data, 30);
        int missing = BasicPrograms.binarySearch(data, 35);
        return List.of("Array : " + Arrays.toString(data), "Search: 30",
                found >= 0 ? "Found at index " + found : "Not found",
                "Search: 35", missing >= 0 ? "Found at index " + missing : "Not found");
    }

    public static List<String> selectionSort() {
        int[] data = {5, 2, 4, 1, 3};
        return List.of("Input : " + Arrays.toString(data), "Output: " + Arrays.toString(BasicPrograms.selectionSort(data)));
    }

    public static List<String> insertionSort() {
        int[] data = {5, 2, 4, 1, 3};
        return List.of("Input : " + Arrays.toString(data), "Output: " + Arrays.toString(BasicPrograms.insertionSort(data)));
    }

    // ====================================================================== 2. OOP
    public static List<String> oop() {
        List<String> out = new ArrayList<>();
        Student rahul = new Student(101, "Rahul", "rahul@college.edu", "9000000101", 1, "CSE", 1, "CSE-III-A", 2025);
        out.add("CLASS & OBJECT : Student rahul = new Student(101, \"Rahul\", ...)");
        out.add("ENCAPSULATION  : private fields, access through rahul.getName() = " + rahul.getName());
        rahul.setName("Rahul K");
        out.add("                 after rahul.setName(\"Rahul K\") -> " + rahul);
        Faculty kumar = new Faculty(1, "F001", "Kumar", "kumar@college.edu", "Professor", 1, "CSE");
        List<Person> people = List.of(rahul, kumar);
        out.add("INHERITANCE    : Student and Faculty both extend Person");
        for (Person p : people) {
            out.add("POLYMORPHISM   : Person p = " + p.getClass().getSimpleName() + " -> p.getRole() = " + p.getRole());
        }
        out.add("ABSTRACTION    : 'new Person(...)' does not compile because Person is abstract");
        out.add("Where used (LEVEL 2): model/Student.java is built from every row of table STUDENT");
        return out;
    }

    // ====================================================================== 3. EMPLOYEE PAYSLIP
    public static List<String> payslip() {
        List<String> out = new ArrayList<>();
        Employee professor = new Professor(1001, "Dr. Ramesh Iyer", new BigDecimal("80000"));
        out.add("Employee e = new Professor(1001, \"Dr. Ramesh Iyer\", 80000)");
        out.add("HRA = " + professor.hra() + "   DA = " + professor.da() + "   PF = " + professor.pf());
        out.add("Gross = 80000 + " + professor.hra() + " + " + professor.da() + " = " + professor.gross());
        out.add("Net   = " + professor.gross() + " - " + professor.pf() + " = " + professor.net());
        out.add("");
        out.addAll(PayslipFormatter.toLines(PayrollCalculator.payslip(professor, "2026-10")));
        out.add("");
        out.add("POLYMORPHISM: the same call e.net() gives a different answer for each subclass:");
        List<Employee> staff = List.of(
                new Programmer(1, "Programmer", new BigDecimal("45000")),
                new AssistantProfessor(2, "Asst. Professor", new BigDecimal("50000")),
                new AssociateProfessor(3, "Assoc. Professor", new BigDecimal("65000")),
                new Professor(4, "Professor", new BigDecimal("80000")));
        for (Employee e : staff) {
            out.add(String.format("  %-20s basic=%9s gross=%10s net=%10s", e.designation().label(),
                    e.basic(), e.gross(), e.net()));
        }
        out.add("Where used (LEVEL 2): PayrollService builds an Employee subclass from every EMPLOYEE row");
        return out;
    }

    // ====================================================================== 4. ABSTRACT CLASS
    public static List<String> shapes() {
        List<String> out = new ArrayList<>();
        out.add("abstract class Shape { int a; int b; abstract void printArea(); }");
        Shape[] shapes = {new Rectangle(10, 5), new Triangle(10, 5), new Circle(3)};
        for (Shape s : shapes) {
            // polymorphism: the variable type is Shape, Java runs the printArea() of the real object
            out.add(String.format("%s -> area = %.2f", s.shapeName(), s.area()));
            s.printArea();   // also prints to the console
        }
        return out;
    }

    // ====================================================================== 5. INTERFACE
    public static List<String> interfaces() {
        List<String> out = new ArrayList<>();
        Payable p = new Professor(1, "Professor", new BigDecimal("80000"));
        out.add("Payable p = new Professor(...)   (interface type, object of a class that implements it)");
        out.add("p.basic() = " + p.basic() + ", p.pf() = " + p.pf());
        out.add("p.net()  = " + p.net() + "   <- 'default' method written inside the interface");
        Searchable<String> bySubstring = keyword -> List.of("Rahul", "Priya", "Arun").stream()
                .filter(n -> StringUtil.containsIgnoreCase(n, keyword)).toList();
        out.add("Searchable<String> (lambda implementation) search(\"ri\") = " + bySubstring.search("ri"));
        out.add("Where used (LEVEL 2): Payable <- Employee,  Searchable<Student> <- StudentService,  ReportGenerator <- ReportService");
        return out;
    }

    // ====================================================================== 6. EXCEPTIONS
    public static List<String> exceptions() {
        List<String> out = new ArrayList<>();
        try {                                           // 1. user-defined exception
            MarkValidator.validate("CT1", new BigDecimal("31"), new BigDecimal("30"));
        } catch (InvalidMarksException e) {
            out.add("InvalidMarksException  : " + e.getMessage());
        }
        try {                                           // 2. built-in unchecked exception
            BasicPrograms.elementAt(new int[]{1, 2, 3}, 10);
        } catch (ArrayIndexOutOfBoundsException e) {
            out.add("Invalid array index    : " + e.getMessage());
        }
        try {
            int zero = 0;
            out.add("" + (10 / zero));
        } catch (ArithmeticException e) {
            out.add("ArithmeticException    : " + e.getMessage());
        }
        try {
            Integer.parseInt("12abc");
        } catch (NumberFormatException e) {
            out.add("NumberFormatException  : " + e.getMessage());
        }
        try {                                           // 3. checked exception (must be caught or declared)
            FileUtil.countCharacters(Path.of("no-such-file.txt"));
        } catch (IOException e) {
            out.add("File not found (IOException): " + e.getClass().getSimpleName());
        }
        try {                                           // 4. multi-catch + finally
            Object o = null;
            o.toString();
        } catch (NullPointerException | IllegalStateException e) {
            out.add("Multi-catch            : " + e.getClass().getSimpleName());
        } finally {
            out.add("finally block always runs (used to close resources)");
        }
        out.add("Where used (LEVEL 2): GlobalExceptionHandler turns these exceptions into HTTP 400/404/409 JSON");
        return out;
    }

    // ====================================================================== 7. STRING HANDLING
    public static List<String> strings() {
        List<String> out = new ArrayList<>();
        String raw = "  rahul   KUMAR ";
        out.add("toTitleCase(\"" + raw + "\") = \"" + StringUtil.toTitleCase(raw) + "\"");
        out.add("normalizeSubjectCode(\" cs302 \") = " + StringUtil.normalizeSubjectCode(" cs302 "));
        out.add("isValidSubjectCode(\"CS302\") = " + StringUtil.isValidSubjectCode("CS302")
                + ",  isValidSubjectCode(\"3CS\") = " + StringUtil.isValidSubjectCode("3CS"));
        out.add("isValidEmail(\"rahul@college.edu\") = " + StringUtil.isValidEmail("rahul@college.edu")
                + ",  isValidEmail(\"rahul@\") = " + StringUtil.isValidEmail("rahul@"));
        out.add("reverse(\"Computer\") = " + StringUtil.reverse("Computer"));
        out.add("isPalindrome(\"Madam\") = " + StringUtil.isPalindrome("Madam"));
        out.add("countVowels(\"Database Management\") = " + StringUtil.countVowels("Database Management"));
        out.add("initials(\"Rahul Kumar Sharma\") = " + StringUtil.initials("Rahul Kumar Sharma"));
        String a = new String("CSE"), b = new String("CSE");
        out.add("== compares references: " + (a == b) + " | equals() compares text: " + a.equals(b));
        out.add("\"CSE\".compareTo(\"ECE\") = " + "CSE".compareTo("ECE") + "  (negative: CSE comes first)");
        StringBuilder sb = new StringBuilder();
        for (String code : new String[]{"CS301", "CS302", "CS303"}) {
            sb.append(code).append(sb.length() < 14 ? ", " : "");
        }
        out.add("StringBuilder joined subject codes: " + sb);
        out.add("split: " + Arrays.toString("CS301,CS302,CS303".split(",")));
        out.add("Where used (LEVEL 2): StudentService validates names/e-mails, SubjectService normalises codes, search box");
        return out;
    }

    // ====================================================================== 8. FILE OPERATIONS
    public static List<String> files(Path directory) throws IOException {
        List<String> out = new ArrayList<>();
        Path report = directory.resolve("report.txt");
        Path copy = directory.resolve("report_copy.txt");
        List<String> lines = List.of("Student report", "Rahul CSE 82.6", "Priya CSE 91.2", "Arun CSE 76.4");
        long written = FileUtil.writeLines(report, lines);
        out.add("1. Created " + report.getFileName() + " (" + written + " characters)");
        out.add("2. Count of letter 'r' in report.txt = " + FileUtil.countOccurrences(report, 'r'));
        out.add("   Count of letter 'a' in report.txt = " + FileUtil.countOccurrences(report, 'a'));
        long copied = FileUtil.copyFile(report, copy);
        out.add("3. Copied to " + copy.getFileName() + " (" + copied + " characters)");
        out.add("4. Copy identical to original: " + Files.readString(report).equals(Files.readString(copy)));
        out.add("Where used (LEVEL 2): ReportService exports every report to a .txt file, then copies / counts it");
        return out;
    }

    // ====================================================================== 9, 11, 12. GENERICS
    public static List<String> generics() {
        List<String> out = new ArrayList<>();
        GenericSearch<Integer> intSearch = new GenericSearch<>();
        Integer[] numbers = {10, 20, 30, 40, 50};
        out.add("GenericSearch<Integer>  search 30 in " + Arrays.toString(numbers) + " -> index " + intSearch.linearSearch(numbers, 30));
        GenericSearch<String> strSearch = new GenericSearch<>();
        String[] codes = {"CS301", "CS302", "CS303"};
        out.add("GenericSearch<String>   search CS303 in " + Arrays.toString(codes) + " -> index " + strSearch.linearSearch(codes, "CS303"));

        GenericSearch<Student> studentSearch = new GenericSearch<>();
        List<Student> students = sampleStudents();
        List<Student> hits = studentSearch.filter(students, s -> StringUtil.containsIgnoreCase(s.getName(), "ri"));
        out.add("GenericSearch<Student>  names containing \"ri\" -> " + hits);

        out.add("GenericAverage<Integer> {10,20,30}     = " + new GenericAverage<>(List.of(10, 20, 30)).average());
        out.add("GenericAverage<Double>  {82.6,91.2,76.4} = " + new GenericAverage<>(List.of(82.6, 91.2, 76.4)).average());
        out.add("GenericAverage<BigDecimal> {82.6, 91.2}  = " + new GenericAverage<>(List.of(new BigDecimal("82.6"), new BigDecimal("91.2"))).average());
        Pair<String, Integer> p = new Pair<>("CS302", 4);
        out.add("Pair<String,Integer> " + p + "  swap() -> " + p.swap());
        out.add("Where used (LEVEL 2): StudentService.search() uses GenericSearch<Student>; reports use GenericAverage");
        return out;
    }

    // ====================================================================== 15. COLLECTIONS
    public static List<String> collections() {
        List<String> out = new ArrayList<>();
        List<Student> list = sampleStudents();                                  // List  : ordered, duplicates allowed
        out.add("List<Student>  size = " + list.size() + " : " + list);
        Collections.sort(list, Comparator.comparing(Student::getName));         // sort with a Comparator
        out.add("sorted by name : " + list);
        Collections.sort(list);                                                  // natural order (Comparable -> by id)
        out.add("sorted by id   : " + list);

        Map<Long, Student> byId = new HashMap<>();                              // Map   : key -> value
        for (Student s : list) {
            byId.put(s.getId(), s);
        }
        out.add("Map<Long,Student> get(102) = " + byId.get(102L));
        out.add("Map containsKey(999) = " + byId.containsKey(999L));

        Set<String> depts = new TreeSet<>();                                    // Set   : no duplicates, TreeSet sorted
        for (Student s : list) {
            depts.add(s.getDeptCode());
        }
        out.add("Set<String> of department codes (duplicates removed, sorted) = " + depts);
        Set<Integer> dupes = new HashSet<>(List.of(1, 2, 2, 3, 3, 3));
        out.add("HashSet of [1,2,2,3,3,3] size = " + dupes.size());

        Iterator<Student> it = list.iterator();                                 // Iterator: safe removal while looping
        while (it.hasNext()) {
            if (it.next().getId() == 103) {
                it.remove();
            }
        }
        out.add("after Iterator.remove(103): " + list);
        out.add("Where used (LEVEL 2): StudentService works with List<Student>; reports build Map<String,Long> counts");
        return out;
    }

    // ====================================================================== 10. MULTITHREADING
    public static List<String> threads() throws InterruptedException {
        List<String> out = Collections.synchronizedList(new ArrayList<>());

        // (a) extend Thread
        class ReportThread extends Thread {
            ReportThread(String name) { super(name); }
            @Override public void run() { out.add("(a) Thread subclass  [" + getName() + "] generating attendance report"); }
        }
        ReportThread t1 = new ReportThread("report-thread-1");
        // (b) implement Runnable
        Thread t2 = new Thread(() -> out.add("(b) Runnable lambda   [" + Thread.currentThread().getName() + "] generating result report"),
                "report-thread-2");
        t1.start();
        t2.start();
        t1.join();                       // wait until the thread finished
        t2.join();

        // (c) ExecutorService: a pool of worker threads (used by ReportService)
        ExecutorService pool = Executors.newFixedThreadPool(2);
        AtomicInteger finished = new AtomicInteger();       // thread-safe counter
        List<Future<String>> futures = new ArrayList<>();
        for (String type : List.of("PAYROLL", "DEPARTMENT")) {
            futures.add(pool.submit(() -> {
                Thread.sleep(50);                              // pretend to work
                finished.incrementAndGet();
                return "(c) Executor task    [" + Thread.currentThread().getName() + "] finished " + type + " report";
            }));
        }
        for (Future<String> f : futures) {
            try {
                out.add(f.get());
            } catch (Exception e) {
                out.add("task failed: " + e.getMessage());
            }
        }
        pool.shutdown();
        pool.awaitTermination(2, TimeUnit.SECONDS);
        out.add("Both executor tasks done, counter = " + finished.get() + " (AtomicInteger is safe for many threads)");
        out.add("Note: lines (a) and (b) can appear in either order - the threads really run at the same time.");
        out.add("Where used (LEVEL 2): ReportService.generateParallel() builds several reports at the same time");
        return new ArrayList<>(out);
    }

    // ====================================================================== 13. BUILT-IN PACKAGES / 14. USER-DEFINED PACKAGES
    public static List<String> packages() {
        List<String> out = new ArrayList<>();
        out.add("BUILT-IN packages used in this project:");
        out.add("  java.lang  : Math.sqrt(144) = " + Math.sqrt(144) + ", String, StringBuilder, Integer.parseInt(\"42\") = " + Integer.parseInt("42"));
        out.add("  java.util  : ArrayList, HashMap, TreeSet, Arrays.toString, Collections.sort");
        out.add("  java.io    : FileReader / BufferedWriter in FileUtil (class files: " + FileUtil.class.getSimpleName() + ")");
        out.add("  java.nio   : Path.of(\"output/report.txt\") = " + Path.of("output", "report.txt"));
        out.add("  java.math  : new BigDecimal(\"0.1\").add(new BigDecimal(\"0.2\")) = " + new BigDecimal("0.1").add(new BigDecimal("0.2")));
        LocalDate day = LocalDate.of(2026, 10, 8);
        out.add("  java.time  : LocalDate.of(2026, 10, 8) = " + day + " is a " + day.getDayOfWeek());
        out.add("  java.text  : new DecimalFormat(\"#,##0.00\").format(104000) = " + new DecimalFormat("#,##0.00").format(104000));
        out.add("  java.util.concurrent : ExecutorService in the multithreading demo");
        out.add("USER-DEFINED packages (folders under src/main/java/com/college):");
        out.add("  " + BasicPrograms.class.getPackageName() + "   -> BasicPrograms, Shape, Rectangle, Triangle, Circle");
        out.add("  " + Employee.class.getPackageName() + "   -> Employee, Programmer, AssistantProfessor, AssociateProfessor, Professor, Payable");
        out.add("  " + PayrollCalculator.class.getPackageName() + "   -> PayrollCalculator, Payslip, PayslipFormatter");
        out.add("  " + com.college.marks.MarkCalculator.class.getPackageName() + "   -> MarkCalculator, MarkValidator");
        out.add("  " + com.college.attendance.AttendanceCalculator.class.getPackageName() + "   -> AttendanceCalculator, AttendanceStatus");
        out.add("  " + com.college.result.GradeScale.class.getPackageName() + "   -> GradeScale, GradePointCalculator");
        out.add("  " + Student.class.getPackageName() + "   -> Student, Faculty, Person, records ...");
        out.add("  " + StringUtil.class.getPackageName() + "   -> StringUtil, FileUtil, GenericSearch, GenericAverage");
        out.add("  " + InvalidMarksException.class.getPackageName() + "   -> user-defined exceptions");
        out.add("A class in another package is used with: import com.college.util.StringUtil;");
        return out;
    }

    // ====================================================================== helpers
    private static List<Student> sampleStudents() {
        return new ArrayList<>(List.of(
                new Student(103, "Arun", "arun@college.edu", null, 1, "CSE", 1, "CSE-III-A", 2025),
                new Student(101, "Rahul", "rahul@college.edu", null, 1, "CSE", 1, "CSE-III-A", 2025),
                new Student(105, "Karthik", "karthik@college.edu", null, 2, "ECE", 2, "ECE-III-A", 2025),
                new Student(102, "Priya", "priya@college.edu", null, 1, "CSE", 1, "CSE-III-A", 2025)));
    }

    /** Runs every demo and returns title -> lines (used by the terminal runner and the REST API). */
    public static Map<String, List<String>> all(Path tempDirectory) throws IOException, InterruptedException {
        Map<String, List<String>> map = new java.util.LinkedHashMap<>();
        map.put("1a Factorial", factorial());
        map.put("1b Fibonacci", fibonacci());
        map.put("1c Binary search", binarySearch());
        map.put("1d Selection sort", selectionSort());
        map.put("1e Insertion sort", insertionSort());
        map.put("2 OOP principles", oop());
        map.put("3 Employee payslip (inheritance)", payslip());
        map.put("4 Abstract class Shape", shapes());
        map.put("5 Interfaces", interfaces());
        map.put("6 Exception handling", exceptions());
        map.put("7 String handling", strings());
        map.put("8 File operations", files(tempDirectory));
        map.put("9,11,12 Generics (class, search, average)", generics());
        map.put("10 Multithreading", threads());
        map.put("13,14 Packages", packages());
        map.put("15 Collections", collections());
        return map;
    }
}
