package com.college.dto;

import java.math.BigDecimal;

public record EmployeeRequest(Long id, String name, String email, String designation, BigDecimal basicSalary) {
}
