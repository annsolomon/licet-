package com.college.basics;

/** Circle: a = radius (b is not needed and is set to 0). */
public class Circle extends Shape {

    public Circle(int radius) {
        super(radius, 0);
    }

    @Override
    public void printArea() {
        System.out.printf("Area of Circle (PI x %d x %d) = %.2f%n", a, a, area());
    }

    @Override
    public double area() {
        return Math.PI * a * a;
    }
}
