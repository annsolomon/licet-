package com.college.model;

/**
 * SYLLABUS: OOP - ABSTRACTION + INHERITANCE.
 *
 * Person is the common parent of Student and Faculty. It is abstract: you cannot create
 * a "plain" Person, only a Student or a Faculty. It holds what both have in common.
 *
 * ENCAPSULATION: fields are private; the outside world uses getters / setters, which is
 * where validation can be added.
 */
public abstract class Person {

    private long id;
    private String name;
    private String email;

    protected Person(long id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    /** Every subclass states its own role (method overriding / polymorphism). */
    public abstract String getRole();

    public long getId()            { return id; }
    public void setId(long id)     { this.id = id; }

    public String getName()        { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail()       { return email; }
    public void setEmail(String email) { this.email = email; }

    @Override
    public String toString() {
        return getRole() + "[" + id + ", " + name + "]";
    }
}
