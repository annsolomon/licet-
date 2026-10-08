package com.college.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * SYLLABUS: Generic classes + GENERIC SEARCH.
 *
 * LEVEL 1 (tiny example)  : GenericSearch&lt;Integer&gt; finds 30 in {10,20,30,40,50} -> index 2
 * LEVEL 2 (real project)  : GenericSearch&lt;Student&gt; filters the student list by id / name
 *
 * "T" is a placeholder for ANY type. The same code works for Integer, String, Student ...
 * and the compiler checks the types, so no casting is needed.
 *
 * @param <T> element type
 */
public class GenericSearch<T> {

    /** Linear search in an array. @return index of the key or -1 */
    public int linearSearch(T[] items, T key) {
        for (int i = 0; i < items.length; i++) {
            if (Objects.equals(items[i], key)) {
                return i;
            }
        }
        return -1;
    }

    /** Linear search in a List. @return index of the key or -1 */
    public int linearSearch(List<T> items, T key) {
        return items.indexOf(key);
    }

    /** Keep only the items accepted by the Predicate (a Predicate is "a yes/no test"). */
    public List<T> filter(Collection<T> items, Predicate<T> test) {
        List<T> found = new ArrayList<>();
        for (T item : items) {
            if (test.test(item)) {
                found.add(item);
            }
        }
        return found;
    }
}
