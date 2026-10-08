package com.college.basics;

/** Rectangle: a = length, b = breadth. Overrides the abstract methods of Shape. */
public class Rectangle extends Shape {

    public Rectangle(int length, int breadth) {
        super(length, breadth);        // calls the Shape constructor
    }

    @Override                          // @Override = "I am replacing a method of the parent"
    public void printArea() {
        System.out.println("Area of Rectangle (" + a + " x " + b + ") = " + area());
    }

    @Override
    public double area() {
        return a * b;
    }
}
