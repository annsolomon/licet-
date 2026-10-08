package com.college.basics;

/**
 * SYLLABUS: ABSTRACT CLASS (exact syllabus form).
 *
 *     Shape  (abstract)  int a; int b;  abstract printArea()
 *       |-- Rectangle      area = a * b
 *       |-- Triangle       area = 1/2 * a * b      (a = base, b = height)
 *       '-- Circle         area = PI * a * a       (a = radius, b unused)
 *
 * ABSTRACT CLASS : a class that cannot be instantiated (new Shape() is a compile error);
 *                  it defines WHAT every shape must do, not HOW.
 * ABSTRACTION    : the caller only knows "a Shape can print its area".
 * INHERITANCE    : Rectangle/Triangle/Circle "extends" Shape and reuse fields a and b.
 * OVERRIDING     : each subclass re-implements printArea() / area() in its own way.
 * POLYMORPHISM   : Shape s = new Circle(..); s.printArea();  -> Java decides at RUN TIME
 *                  which printArea() to execute (the Circle one).
 */
public abstract class Shape {

    protected int a;
    protected int b;

    protected Shape(int a, int b) {
        this.a = a;
        this.b = b;
    }

    /** Syllabus method: every concrete shape MUST implement it (no body here). */
    public abstract void printArea();

    /** Same value as a number, so the REST API can return it. */
    public abstract double area();

    /** Name used in messages. */
    public String shapeName() {
        return getClass().getSimpleName();
    }
}
