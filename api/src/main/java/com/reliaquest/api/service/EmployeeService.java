package com.reliaquest.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reliaquest.api.dto.ApiResponse;
import com.reliaquest.api.dto.EmployeeDTO;
import com.reliaquest.api.dto.EmployeesResponse;
import com.reliaquest.api.exception.ResourceNotFoundException;
import com.reliaquest.api.model.Employee;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Employee Service to provide the operations related to employees.
 */
@Service
public class EmployeeService implements IEmployeeService {

    private static final Logger logger = LoggerFactory.getLogger(EmployeeService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final RestTemplate restTemplate;

    @Value("${mock-employee.url}")
    private String mockEmployeeBaseUrl;

    /**
     * Constructor that injects the RestTemplate to communicate with external services.
     *
     * @param restTemplate the RestTemplate object used to make HTTP requests.
     */
    @Autowired
    public EmployeeService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Retrieves all employees from the external API.
     *
     * @return a list of Employee objects.
     * @throws RuntimeException if the external API fails to fetch employees.
     */
    @Override
    public List<Employee> getAllEmployees() {
        logger.info("Fetching all employees from the API.");
        ResponseEntity<EmployeesResponse> response = restTemplate.exchange(
                mockEmployeeBaseUrl, HttpMethod.GET, null, new ParameterizedTypeReference<>() {
                });
        if (response.getStatusCode() == HttpStatus.OK) {
            return response.getBody().getData();
        } else {
            logger.error("Failed to fetch employees: {}", response.getStatusCode());
            throw new RuntimeException("Failed to fetch employees");
        }
    }

    /**
     * Searches for employees whose name contains the provided search string.
     *
     * @param searchString the name or part of the name to search for.
     * @return a list of employees whose name matches the search string.
     */
    @Override
    public List<Employee> getEmployeesByNameSearch(String searchString) {
        logger.info("Searching for employees with name containing: {}", searchString);
        List<Employee> employees = getAllEmployees();
        return employees.stream()
                .filter(employee -> employee.getEmployeeName().toLowerCase().contains(searchString.toLowerCase()))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a single employee by their unique ID.
     *
     * @param id the ID of the employee to retrieve.
     * @return the Employee object.
     * @throws ResourceNotFoundException if the employee with the specified ID is not found.
     * @throws RuntimeException if the external API call fails.
     */
    @Override
    public Employee getEmployeeById(String id) {
        logger.info("Fetching employee with ID: {}", id);
        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(mockEmployeeBaseUrl).pathSegment(id);
        ResponseEntity<ApiResponse<Employee>> response = restTemplate.exchange(
                uriComponentsBuilder.toUriString(), HttpMethod.GET, null, new ParameterizedTypeReference<>() {
                });
        if (response.getStatusCode() == HttpStatus.OK) {
            return response.getBody() != null ? response.getBody().getData() : null;
        } else if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
            logger.error("Employee with ID {} not found.", id);
            throw new ResourceNotFoundException("Employee not found with ID: " + id);
        } else {
            logger.error("Failed to fetch employee: {}", response.getStatusCode());
            throw new RuntimeException("Failed to fetch employee");
        }
    }

    /**
     * Retrieves the highest salary among all employees.
     *
     * @return the highest salary value.
     */
    @Override
    public Integer getHighestSalaryOfEmployees() {
        logger.info("Fetching highest salary among employees.");
        List<Employee> employees = getAllEmployees();
        return employees.stream()
                .map(Employee::getEmployeeSalary)
                .max(Integer::compareTo)
                .orElse(0);
    }

    /**
     * Retrieves the names of the top ten highest earning employees.
     *
     * @return a list of names of the top ten highest earning employees.
     */
    @Override
    public List<String> getTopTenHighestEarningEmployeeNames() {
        logger.info("Fetching top ten highest earning employees.");
        List<Employee> employees = getAllEmployees();
        return employees.stream()
                .sorted((emp1, emp2) ->
                        Integer.compare(emp2.getEmployeeSalary(), emp1.getEmployeeSalary())) // Sort in descending order
                .limit(10)
                .map(Employee::getEmployeeName)
                .collect(Collectors.toList());
    }

    /**
     * Creates a new employee by posting the provided employee data to the external API.
     *
     * @param employeeInput the employee data to be created.
     * @return the created Employee object.
     * @throws IllegalArgumentException if the employee data is invalid.
     * @throws RuntimeException if the external API call fails.
     */
    @Override
    public Employee createEmployee(EmployeeDTO employeeInput) {
        validateEmployeeInput(employeeInput);

        logger.info("Creating a new employee: {}", employeeInput.getName());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<EmployeeDTO> request = new HttpEntity<>(employeeInput, headers);

        ResponseEntity<ApiResponse<Employee>> response = restTemplate.exchange(
                mockEmployeeBaseUrl, HttpMethod.POST, request, new ParameterizedTypeReference<>() {
                });
        if (response.getStatusCode() == HttpStatus.OK) {
            return response.getBody() != null ? response.getBody().getData() : null;
        } else {
            logger.error("Failed to create employee: {}", response.getStatusCode());
            throw new RuntimeException("Failed to create employee");
        }
    }

    /**
     * Validates the provided employee input data.
     *
     * @param employeeInput the employee data to validate.
     * @throws IllegalArgumentException if any validation fails.
     */
    private void validateEmployeeInput(EmployeeDTO employeeInput) {
        // Check for null input
        Objects.requireNonNull(employeeInput, "Employee input must not be null");

        // Check employee name
        if (employeeInput.getName() == null
                || employeeInput.getName().isBlank()) {
            throw new IllegalArgumentException("Employee name must not be blank");
        }

        // Check employee salary
        if (employeeInput.getSalary() <= 0) {
            throw new IllegalArgumentException("Employee salary must be greater than zero");
        }

        // Check employee age
        if (employeeInput.getAge() < 16 || employeeInput.getAge() > 75) {
            throw new IllegalArgumentException("Employee age must be between 16 and 75");
        }

        // Check employee title
        if (employeeInput.getTitle() == null
                || employeeInput.getTitle().isBlank()) {
            throw new IllegalArgumentException("Employee title must not be blank");
        }
    }

    /**
     * Deletes an employee by their unique ID.
     *
     * @param id the ID of the employee to delete.
     * @return the name of the deleted employee.
     * @throws ResourceNotFoundException if the employee with the specified ID is not found.
     * @throws RuntimeException if the external API call fails.
     */
    @Override
    public String deleteEmployeeById(String id) {
        Objects.requireNonNull(id, "Employee id must not be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("Employee id must not be blank");
        }
        logger.info("Deleting employee with ID: {}", id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> reqBody = new HashMap<>();
        Employee employee = getEmployeeById(id);
        if (employee == null) {
            throw new ResourceNotFoundException("Employee not found with the given id : " + id);
        }
        reqBody.put("name", employee.getEmployeeName());

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(reqBody, headers);
        ResponseEntity<ApiResponse<Boolean>> response = restTemplate.exchange(
                mockEmployeeBaseUrl,
                HttpMethod.DELETE,
                requestEntity,
                new ParameterizedTypeReference<>() {
                });
        if (response.getBody() != null && response.getBody().getData() != null && response.getBody().getData()) {
            return employee.getEmployeeName();
        } else {
            throw new RuntimeException("Failed to delete employee: " + response.getBody().getStatus());
        }
    }
}
