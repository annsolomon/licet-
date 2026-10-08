package com.college.basics;

/** Triangle: a = base, b = height. */
public class Triangle extends Shape {

    public Triangle(int base, int height) {
        super(base, height);
    }

    @Override
    public void printArea() {
        System.out.println("Area of Triangle (1/2 x " + a + " x " + b + ") = " + area());
    }

    @Override
    public double area() {
        return 0.5 * a * b;
    }
}
