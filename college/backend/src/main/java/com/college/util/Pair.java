package com.college.util;

/**
 * SYLLABUS: GENERIC CLASS with two type parameters.
 *
 * A Pair holds two related values of any types, e.g. Pair&lt;String,Integer&gt; ("CS302", 4)
 * = subject code and credits. Used in the Java Lab and when returning "name + value" pairs.
 *
 * @param <A> type of the first value
 * @param <B> type of the second value
 */
public class Pair<A, B> {

    private final A first;
    private final B second;

    public Pair(A first, B second) {
        this.first = first;
        this.second = second;
    }

    public A getFirst()  { return first; }
    public B getSecond() { return second; }

    /** Returns a new pair with the values swapped: Pair&lt;B,A&gt;. */
    public Pair<B, A> swap() {
        return new Pair<>(second, first);
    }

    @Override
    public String toString() {
        return "(" + first + ", " + second + ")";
    }
}
