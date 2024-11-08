package com.reliaquest.api.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.reliaquest.api.model.Employee;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeesResponse {
    @JsonProperty("data")
    private List<Employee> data;

    private String status;
}
