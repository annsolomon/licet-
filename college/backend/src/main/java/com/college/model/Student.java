package com.college.model;

import java.util.Objects;

/**
 * SYLLABUS: OOP - CLASS and OBJECT.
 *
 * A Student object = one row of table STUDENT.
 * The repository's RowMapper builds one Student per ResultSet row:
 *     ResultSet row  ->  new Student(...)  ->  JSON  ->  React table row
 *
 * Implements Comparable so a List&lt;Student&gt; can be sorted with Collections.sort (by id).
 */
public class Student extends Person implements Comparable<Student> {

    private String phone;
    private long deptId;
    private String deptCode;
    private long classId;
    private String className;
    private Integer joinedYear;

    public Student(long id, String name, String email, String phone,
                   long deptId, String deptCode, long classId, String className, Integer joinedYear) {
        super(id, name, email);              // parent constructor stores id, name, email
        this.phone = phone;
        this.deptId = deptId;
        this.deptCode = deptCode;
        this.classId = classId;
        this.className = className;
        this.joinedYear = joinedYear;
    }

    @Override
    public String getRole() {
        return "STUDENT";
    }

    public String getPhone()        { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public long getDeptId()         { return deptId; }
    public String getDeptCode()     { return deptCode; }
    public long getClassId()        { return classId; }
    public String getClassName()    { return className; }
    public Integer getJoinedYear()  { return joinedYear; }

    @Override
    public int compareTo(Student other) {
        return Long.compare(getId(), other.getId());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Student s)) {
            return false;
        }
        return getId() == s.getId();         // two students are the same when the id is the same
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}
