package com.college.util;

import java.util.List;

/**
 * SYLLABUS: Interfaces.
 *
 * An interface is a CONTRACT: it lists method names without bodies. Any class that
 * "implements" it must provide those methods.
 *
 * Here: anything that can be searched by a keyword. StudentService implements
 * Searchable&lt;Student&gt;, so the contract guarantees it has search(String).
 *
 * @param <T> the type of object that is found (generics, see GenericSearch)
 */
public interface Searchable<T> {

    /** @return all objects that match the keyword (never null) */
    List<T> search(String keyword);
}
