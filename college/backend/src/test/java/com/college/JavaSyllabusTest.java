package com.college;

import com.college.basics.BasicPrograms;
import com.college.basics.Circle;
import com.college.basics.Rectangle;
import com.college.basics.Shape;
import com.college.basics.Triangle;
import com.college.util.FileUtil;
import com.college.util.GenericAverage;
import com.college.util.GenericSearch;
import com.college.util.Pair;
import com.college.util.StringUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JavaSyllabusTest {

    @Test
    void factorialAndFibonacci() {
        assertEquals(120, BasicPrograms.factorial(5));
        assertEquals(120, BasicPrograms.factorialRecursive(5));
        assertEquals(1, BasicPrograms.factorial(0));
        assertEquals(List.of(0L, 1L, 1L, 2L, 3L, 5L, 8L), BasicPrograms.fibonacci(7));
    }

    @Test
    void sortingAndSearching() {
        int[] data = {5, 3, 9, 1, 7};
        assertArrayEquals(new int[]{1, 3, 5, 7, 9}, BasicPrograms.selectionSort(data));
        assertArrayEquals(new int[]{1, 3, 5, 7, 9}, BasicPrograms.insertionSort(data));
        assertEquals(2, BasicPrograms.binarySearch(new int[]{1, 3, 5, 7, 9}, 5));
        assertEquals(-1, BasicPrograms.binarySearch(new int[]{1, 3, 5, 7, 9}, 4));
    }

    @Test
    void arrayIndexIsCheckedByJava() {
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> BasicPrograms.elementAt(new int[]{1, 2}, 5));
    }

    @Test
    void shapesAreAbstractAndPolymorphic() {
        Shape[] shapes = {new Rectangle(4, 5), new Triangle(6, 5), new Circle(2)};
        assertEquals(20.0, shapes[0].area(), 0.001);
        assertEquals(15.0, shapes[1].area(), 0.001);
        assertEquals(Math.PI * 4, shapes[2].area(), 0.001);
    }

    @Test
    void genericsWorkForAnyType() {
        GenericSearch<String> gs = new GenericSearch<>();
        assertEquals(1, gs.linearSearch(List.of("a", "b", "c"), "b"));
        assertEquals(-1, gs.linearSearch(new String[]{"a"}, "z"));
        assertEquals(List.of("bb", "cc"), gs.filter(List.of("a", "bb", "cc"), s -> s.length() == 2));
        assertEquals(2.5, new GenericAverage<>(List.of(1, 2, 3, 4)).average(), 0.0001);
        Pair<String, Integer> pair = new Pair<>("x", 1);
        assertEquals(1, (int) pair.swap().getFirst());
    }

    @Test
    void stringHelpers() {
        assertEquals("Rahul Kumar", StringUtil.toTitleCase("rahul kumar"));
        assertEquals("CS302", StringUtil.normalizeSubjectCode(" cs302 "));
        assertTrue(StringUtil.isPalindrome("Madam"));
        assertEquals(2, StringUtil.countVowels("Rahul"));
        assertTrue(StringUtil.isValidEmail("a@b.edu"));
        assertFalse(StringUtil.isValidEmail("nope"));
        assertEquals("luhaR", StringUtil.reverse("Rahul"));
    }

    @Test
    void fileWriteCountCopy(@TempDir Path dir) throws IOException {
        Path a = dir.resolve("a.txt");
        FileUtil.writeLines(a, List.of("banana", "bandana"));
        assertEquals(6, FileUtil.countOccurrences(a, 'a'));
        Path b = dir.resolve("b.txt");
        FileUtil.copyFile(a, b);
        assertEquals(FileUtil.countCharacters(a), FileUtil.countCharacters(b));
        assertEquals(List.of("banana", "bandana"), FileUtil.readLines(b));
        assertTrue(Files.exists(b));
    }
}
