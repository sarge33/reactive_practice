package com.example.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
//@RequiredArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class Employee {
    private int id;
    private String name;
    private int salary;
}