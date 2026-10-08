package com.college.basics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * SYLLABUS: Simple Java programs - Factorial, Fibonacci, Binary Search,
 * Selection Sort, Insertion Sort.
 *
 * Each method is tiny and has a documented example so it can be run in the viva:
 *   factorial(5)                      -> 120
 *   fibonacci(8)                      -> [0, 1, 1, 2, 3, 5, 8, 13]
 *   binarySearch({10,20,30,40,50},30) -> 2
 *   selectionSort({5,2,4,1,3})        -> [1, 2, 3, 4, 5]
 *   insertionSort({5,2,4,1,3})        -> [1, 2, 3, 4, 5]
 *
 * Project use: the Java Lab page (REST /api/lab/basics) and Student list sorting.
 */
public final class BasicPrograms {

    private BasicPrograms() { }

    /** n! = 1 x 2 x ... x n.  Uses long (works up to 20!). Throws for negative n. */
    public static long factorial(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("Factorial is not defined for negative numbers: " + n);
        }
        if (n > 20) {
            throw new IllegalArgumentException("n must be <= 20 (long overflows after 20!)");
        }
        long result = 1;
        for (int i = 2; i <= n; i++) {
            result *= i;
        }
        return result;
    }

    /** Recursive version: factorial(n) = n * factorial(n-1). Shown to explain recursion. */
    public static long factorialRecursive(int n) {
        if (n < 0 || n > 20) {
            throw new IllegalArgumentException("n must be between 0 and 20");
        }
        return (n <= 1) ? 1 : n * factorialRecursive(n - 1);
    }

    /** First 'count' Fibonacci numbers: each number is the sum of the previous two. */
    public static List<Long> fibonacci(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count must not be negative");
        }
        List<Long> series = new ArrayList<>();
        long a = 0, b = 1;
        for (int i = 0; i < count; i++) {
            series.add(a);
            long next = a + b;
            a = b;
            b = next;
        }
        return series;
    }

    /**
     * Binary search on a SORTED array: look at the middle, discard the half that cannot contain the key.
     * @return index of key, or -1 when absent
     */
    public static int binarySearch(int[] sorted, int key) {
        int low = 0, high = sorted.length - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;          // middle index (unsigned shift avoids overflow)
            if (sorted[mid] == key) {
                return mid;
            } else if (sorted[mid] < key) {
                low = mid + 1;                      // key is in the right half
            } else {
                high = mid - 1;                     // key is in the left half
            }
        }
        return -1;
    }

    /** Selection sort: repeatedly pick the smallest remaining element and swap it into place. Works on a copy. */
    public static int[] selectionSort(int[] input) {
        int[] a = Arrays.copyOf(input, input.length);
        for (int i = 0; i < a.length - 1; i++) {
            int min = i;
            for (int j = i + 1; j < a.length; j++) {
                if (a[j] < a[min]) {
                    min = j;
                }
            }
            int temp = a[i];
            a[i] = a[min];
            a[min] = temp;
        }
        return a;
    }

    /** Insertion sort: take each element and insert it into the already sorted left part. Works on a copy. */
    public static int[] insertionSort(int[] input) {
        int[] a = Arrays.copyOf(input, input.length);
        for (int i = 1; i < a.length; i++) {
            int key = a[i];
            int j = i - 1;
            while (j >= 0 && a[j] > key) {
                a[j + 1] = a[j];                    // shift bigger elements one place right
                j--;
            }
            a[j + 1] = key;
        }
        return a;
    }

    /**
     * Reads one element of an array - used to demonstrate ArrayIndexOutOfBoundsException handling.
     * @throws ArrayIndexOutOfBoundsException when index is outside 0..length-1
     */
    public static int elementAt(int[] data, int index) {
        return data[index];
    }
}
