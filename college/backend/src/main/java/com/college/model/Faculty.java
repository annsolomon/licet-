package com.college.model;

/** A faculty member = one row of table FACULTY. Inherits id / name / e-mail from Person. */
public class Faculty extends Person {

    private final String code;
    private final String designation;
    private final long deptId;
    private final String deptCode;

    public Faculty(long id, String code, String name, String email, String designation, long deptId, String deptCode) {
        super(id, name, email);
        this.code = code;
        this.designation = designation;
        this.deptId = deptId;
        this.deptCode = deptCode;
    }

    @Override
    public String getRole() {
        return "FACULTY";
    }

    public String getCode()        { return code; }
    public String getDesignation() { return designation; }
    public long getDeptId()        { return deptId; }
    public String getDeptCode()    { return deptCode; }
}
