package com.reliaquest.api.service;

import com.reliaquest.api.dto.ApiResponse;
import com.reliaquest.api.dto.EmployeeDTO;
import com.reliaquest.api.dto.EmployeesResponse;
import com.reliaquest.api.exception.ResourceNotFoundException;
import com.reliaquest.api.model.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    @Spy
    private EmployeeService employeeService;
    private List<Employee> mockEmployees;

    @BeforeEach
    public void setUp() {
        Employee employee1 = new Employee("1", "emp1", 50000, 30, "Engineer", "john.doe@example.com");
        Employee employee2 = new Employee("2", "emp2", 60000, 28, "HR Manager", "jane.smith@example.com");
        Employee employee3 = new Employee("3", "emp3", 70000, 35, "Developer", "alice.johnson@example.com");
        mockEmployees = Arrays.asList(employee1, employee2, employee3);
        ReflectionTestUtils.setField(employeeService, "mockEmployeeBaseUrl", "https://mock.employees.com");
    }

    @Test
    public void testGetAllEmployees_Success() throws Exception {
        EmployeesResponse mockResponse = new EmployeesResponse(mockEmployees, "");
        ResponseEntity<EmployeesResponse> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);

        when(restTemplate.exchange(
                any(String.class),
                eq(HttpMethod.GET),
                isNull(),
                eq(new ParameterizedTypeReference<EmployeesResponse>() {
                })))
                .thenReturn(responseEntity);

        List<Employee> allEmployees = employeeService.getAllEmployees();

        assertNotNull(allEmployees);
        assertEquals(3, allEmployees.size());
        assertEquals("emp1", allEmployees.get(0).getEmployeeName());
        assertEquals("emp2", allEmployees.get(1).getEmployeeName());
        assertEquals(50000, allEmployees.get(0).getEmployeeSalary());
        assertEquals("Engineer", allEmployees.get(0).getEmployeeTitle());
        assertEquals("jane.smith@example.com", allEmployees.get(1).getEmployeeEmail());
    }

    @Test
    void testGetAllEmployees_Failure() {

        ResponseEntity<EmployeesResponse> responseEntity = new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);

        when(restTemplate.exchange(
                any(String.class),
                eq(HttpMethod.GET),
                eq(null),
                eq(new ParameterizedTypeReference<EmployeesResponse>() {
                })
        )).thenReturn(responseEntity);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            employeeService.getAllEmployees();
        });

        assertEquals("Failed to fetch employees", exception.getMessage());
    }

    @Test
    void testGetEmployeeById_Success() {

        Employee mockEmployee = new Employee("1", "emp1", 50000, 30, "Engineer", "john.doe@example.com");
        ApiResponse<Employee> mockResponse = new ApiResponse<>();
        mockResponse.setData(mockEmployee);
        ResponseEntity<ApiResponse<Employee>> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);

        when(restTemplate.exchange(
                any(String.class),
                eq(HttpMethod.GET),
                eq(null),
                eq(new ParameterizedTypeReference<ApiResponse<Employee>>() {
                })
        )).thenReturn(responseEntity);

        Employee result = employeeService.getEmployeeById("1");

        assertNotNull(result);
        assertEquals("emp1", result.getEmployeeName());
        assertEquals(50000, result.getEmployeeSalary());
    }

    @Test
    void testGetEmployeeById_NotFound() {
        ResponseEntity<ApiResponse<Employee>> responseEntity = new ResponseEntity<>(null, HttpStatus.NOT_FOUND);

        when(restTemplate.exchange(
                any(String.class),
                eq(HttpMethod.GET),
                eq(null),
                eq(new ParameterizedTypeReference<ApiResponse<Employee>>() {
                })
        )).thenReturn(responseEntity);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            employeeService.getEmployeeById("3");
        });

        assertEquals("Employee not found with ID: 3", exception.getMessage());
    }

    @Test
    void testGetEmployeeById_Failure() {

        ResponseEntity<ApiResponse<Employee>> responseEntity = new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);

        when(restTemplate.exchange(
                any(String.class),
                eq(HttpMethod.GET),
                eq(null),
                eq(new ParameterizedTypeReference<ApiResponse<Employee>>() {
                })
        )).thenReturn(responseEntity);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            employeeService.getEmployeeById("1");
        });

        assertEquals("Failed to fetch employee", exception.getMessage());
    }

    @Test
    void testGetHighestSalaryOfEmployees_Success() {

        doReturn(mockEmployees).when(employeeService).getAllEmployees();

        Integer highestSalary = employeeService.getHighestSalaryOfEmployees();

        assertNotNull(highestSalary);
        assertEquals(70000, highestSalary);
    }

    @Test
    void testGetTopTenHighestEarningEmployeeNames_Success() {
        doReturn(mockEmployees).when(employeeService).getAllEmployees();

        List<String> topEarningEmployees = employeeService.getTopTenHighestEarningEmployeeNames();

        assertNotNull(topEarningEmployees);
        assertEquals(3, topEarningEmployees.size());
        assertEquals("emp3", topEarningEmployees.get(0));
        assertEquals("emp2", topEarningEmployees.get(1));
        assertEquals("emp1", topEarningEmployees.get(2));
    }

    @Test
    void testCreateEmployee_Success() {
        UUID uuid = UUID.randomUUID();
        EmployeeDTO employeeInput = new EmployeeDTO(uuid, "emp4", 55000, 32, "manager", "emp4@company.com");

        Employee createdEmployee = new Employee(uuid.toString(), "emp4", 55000, 32, "manager", "emp4@company.com");
        ApiResponse<Employee> mockResponse = new ApiResponse<>();
        mockResponse.setData(createdEmployee);
        ResponseEntity<ApiResponse<Employee>> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);

        when(restTemplate.exchange(
                any(String.class),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(new ParameterizedTypeReference<ApiResponse<Employee>>() {
                })
        )).thenReturn(responseEntity);

        Employee result = employeeService.createEmployee(employeeInput);

        assertNotNull(result);
        assertEquals("emp4", result.getEmployeeName());
        assertEquals(55000, result.getEmployeeSalary());
    }


    @Test
    void testCreateEmployee_NullResponse() {

        UUID uuid = UUID.randomUUID();
        EmployeeDTO employeeInput = new EmployeeDTO(uuid, "emp4", 55000, 32, "manager", "emp4@company.com");

        ResponseEntity<ApiResponse<Employee>> responseEntity = new ResponseEntity<>(null, HttpStatus.OK);

        when(restTemplate.exchange(
                any(String.class),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(new ParameterizedTypeReference<ApiResponse<Employee>>() {
                })
        )).thenReturn(responseEntity);

        Employee result = employeeService.createEmployee(employeeInput);

        assertNull(result);
    }

    @Test
    void testCreateEmployee_FailureResponse() {

        UUID uuid = UUID.randomUUID();
        EmployeeDTO employeeInput = new EmployeeDTO(uuid, "emp4", 55000, 32, "manager", "emp4@company.com");

        ResponseEntity<ApiResponse<Employee>> responseEntity = new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);

        when(restTemplate.exchange(
                any(String.class),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(new ParameterizedTypeReference<ApiResponse<Employee>>() {
                })
        )).thenReturn(responseEntity);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            employeeService.createEmployee(employeeInput);
        });

        assertEquals("Failed to create employee", exception.getMessage());
    }

    @Test
    void testCreateEmployee_InvalidInput() {
        UUID uuid = UUID.randomUUID();
        EmployeeDTO employeeInput = new EmployeeDTO(uuid, "", 55000, 32, "manager", "emp4@company.com");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            employeeService.createEmployee(employeeInput);
        });
        assertTrue(exception.getMessage().contains("Employee name must not be blank"));
    }


    @Test
    void testDeleteEmployeeById_Success() {

        String employeeId = "5";
        Employee employee = new Employee("5", "emp5", 60000, 30, "Engineer", "emp5@company.com");
        ApiResponse<Boolean> mockResponse = new ApiResponse<>();
        mockResponse.setData(true);
        ResponseEntity<ApiResponse<Boolean>> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);

        doReturn(employee).when(employeeService).getEmployeeById(employeeId);
        when(restTemplate.exchange(
                any(String.class),
                eq(HttpMethod.DELETE),
                any(HttpEntity.class),
                eq(new ParameterizedTypeReference<ApiResponse<Boolean>>() {
                })
        )).thenReturn(responseEntity);

        String result = employeeService.deleteEmployeeById(employeeId);

        assertEquals("emp5", result);
    }

    @Test
    void testDeleteEmployeeById_NotFound() {

        String employeeId = "5";
        doReturn(null).when(employeeService).getEmployeeById(employeeId);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            employeeService.deleteEmployeeById(employeeId);
        });

        assertEquals("Employee not found with the given id : 5", exception.getMessage());
    }

    @Test
    void testDeleteEmployeeById_FailureResponse() {

        String employeeId = "5";
        Employee employee = new Employee("5", "emp5", 60000, 30, "Engineer", "emp5@company.com");
        ApiResponse<Boolean> mockResponse = new ApiResponse<>();
        mockResponse.setData(false);
        mockResponse.setStatus("Deletion failed");
        ResponseEntity<ApiResponse<Boolean>> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);

        doReturn(employee).when(employeeService).getEmployeeById(employeeId);
        when(restTemplate.exchange(
                any(String.class),
                eq(HttpMethod.DELETE),
                any(HttpEntity.class),
                eq(new ParameterizedTypeReference<ApiResponse<Boolean>>() {
                })
        )).thenReturn(responseEntity);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            employeeService.deleteEmployeeById(employeeId);
        });

        assertEquals("Failed to delete employee: Deletion failed", exception.getMessage());
    }

    @Test
    void testDeleteEmployeeById_NullId() {

        NullPointerException exception = assertThrows(NullPointerException.class, () -> {
            employeeService.deleteEmployeeById(null);
        });

        assertEquals("Employee id must not be null", exception.getMessage());
    }

    @Test
    void testDeleteEmployeeById_BlankId() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            employeeService.deleteEmployeeById(" ");
        });
        assertEquals("Employee id must not be blank", exception.getMessage());
    }
}
