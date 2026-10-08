package com.college.service;

import com.college.basics.BasicPrograms;
import com.college.basics.Circle;
import com.college.basics.Rectangle;
import com.college.basics.Shape;
import com.college.basics.SyllabusDemos;
import com.college.basics.Triangle;
import com.college.exception.ValidationException;
import com.college.model.Student;
import com.college.repository.JdbcDemoRepository;
import com.college.util.StringUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Java Lab: runs the 15 Java syllabus demos and some parameterised ones on request. */
@Service
public class SyllabusLabService {

    private final StudentService students;
    private final JdbcDemoRepository jdbcDemo;
    private final Path reportsDir;

    public SyllabusLabService(StudentService students, JdbcDemoRepository jdbcDemo,
                              @Value("${app.reports.dir:../output}") String reportsDir) {
        this.students = students;
        this.jdbcDemo = jdbcDemo;
        this.reportsDir = Path.of(reportsDir);
    }

    /** All fixed demos. */
    public Map<String, List<String>> all() {
        try {
            Files.createDirectories(reportsDir);
            return SyllabusDemos.all(reportsDir);
        } catch (IOException e) {
            throw new com.college.exception.DatabaseException("File demo failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ValidationException("Thread demo was interrupted");
        }
    }

    public Map<String, Object> factorial(int n) {
        if (n < 0 || n > 20) {
            throw new ValidationException("n must be 0..20");
        }
        return Map.of("n", n, "iterative", BasicPrograms.factorial(n), "recursive", BasicPrograms.factorialRecursive(n));
    }

    public Map<String, Object> fibonacci(int count) {
        if (count < 1 || count > 50) {
            throw new ValidationException("count must be 1..50");
        }
        return Map.of("count", count, "series", BasicPrograms.fibonacci(count));
    }

    public Map<String, Object> sort(String numbers) {
        int[] data = parse(numbers);
        return Map.of("input", data, "selection", BasicPrograms.selectionSort(data),
                "insertion", BasicPrograms.insertionSort(data));
    }

    public Map<String, Object> binarySearch(String numbers, int key) {
        int[] sorted = BasicPrograms.selectionSort(parse(numbers));
        return Map.of("sorted", sorted, "key", key, "index", BasicPrograms.binarySearch(sorted, key));
    }

    public Map<String, Object> shape(String type, int a, int b) {
        Shape s = switch (type == null ? "" : type.toLowerCase()) {
            case "rectangle" -> new Rectangle(a, b);
            case "triangle" -> new Triangle(a, b);
            case "circle" -> new Circle(a);
            default -> throw new ValidationException("type must be rectangle, triangle or circle");
        };
        return Map.of("shape", s.shapeName(), "area", s.area());
    }

    public Map<String, Object> string(String text) {
        if (text == null || text.isBlank()) {
            throw new ValidationException("text is required");
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("text", text);
        m.put("upper", text.toUpperCase());
        m.put("titleCase", StringUtil.toTitleCase(text));
        m.put("reverse", StringUtil.reverse(text));
        m.put("palindrome", StringUtil.isPalindrome(text));
        m.put("vowels", StringUtil.countVowels(text));
        m.put("initials", StringUtil.initials(text));
        return m;
    }

    /** Generic search demo on real students: Java filter vs Oracle LIKE. */
    public Map<String, Object> studentSearch(String keyword) {
        List<Student> java = students.search(keyword);
        List<Student> sql = students.searchSql(keyword);
        return Map.of("keyword", keyword == null ? "" : keyword,
                "javaGenericSearchCount", java.size(), "oracleLikeCount", sql.size(),
                "javaGenericSearch", java.stream().map(Student::getName).toList());
    }

    /** Plain JDBC vs JdbcTemplate: same result, very different amount of code. */
    public Map<String, Object> jdbcCompare(long deptId) {
        List<Student> plain = jdbcDemo.findByDepartmentPlainJdbc(deptId);
        List<Student> template = jdbcDemo.findByDepartmentJdbcTemplate(deptId);
        return Map.of("sql", jdbcDemo.sql(), "plainJdbcRows", plain.size(), "jdbcTemplateRows", template.size(),
                "same", plain.size() == template.size());
    }

    private static int[] parse(String numbers) {
        if (numbers == null || numbers.isBlank()) {
            throw new ValidationException("numbers are required, e.g. 5,3,9,1");
        }
        try {
            List<Integer> list = new ArrayList<>();
            Arrays.stream(numbers.split(",")).map(String::trim).filter(x -> !x.isEmpty())
                    .forEach(x -> list.add(Integer.parseInt(x)));
            if (list.isEmpty() || list.size() > 100) {
                throw new ValidationException("give 1..100 numbers");
            }
            return list.stream().mapToInt(Integer::intValue).toArray();
        } catch (NumberFormatException e) {
            throw new ValidationException("numbers must be integers separated by commas");
        }
    }
}
