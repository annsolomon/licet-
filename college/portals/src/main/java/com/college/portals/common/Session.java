package com.college.portals.common;

/** Who is logged in. linkId = faculty_id (HOD, ADVISOR) or parent_id (PARENT). */
public record Session(String username, String fullName, String role, long linkId) { }
