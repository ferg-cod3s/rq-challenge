package com.reliaquest.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reliaquest.api.model.CreateEmployeeRequest;
import com.reliaquest.api.model.DeleteEmployeeRequest;
import com.reliaquest.api.model.Employee;
import com.reliaquest.api.service.impl.EmployeeService;
import com.reliaquest.api.utils.ApiResponse;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

class EmployeeServiceTest {

    private MockWebServer mockWebServer;
    private EmployeeService employeeService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        // Initialize MockWebServer to simulate the Employee API
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        // Create EmployeeService with default WebClient.Builder
        WebClient.Builder builder = WebClient.builder();
        employeeService = new EmployeeService(builder);

        // Use reflection to replace the private WebClient with one pointing to our mock server
        String mockBaseUrl = mockWebServer.url("/api/v1/employee").toString();
        WebClient mockClient = WebClient.builder().baseUrl(mockBaseUrl).build();

        Field clientField = EmployeeService.class.getDeclaredField("client");
        clientField.setAccessible(true);
        clientField.set(employeeService, mockClient);

        objectMapper = new ObjectMapper();
    }

    @AfterEach
    void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    // Using reflection to inject MockWebServer URL - minimal production code changes
    // This approach maintains existing architecture while enabling comprehensive testing

    @Test
    void deleteEmployee_nullRequest_throwsException() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> employeeService.deleteEmployee(null));
    }

    @Test
    void deleteEmployee_nullEmployeeId_throwsException() {
        // Given
        DeleteEmployeeRequest requestWithNullId = new DeleteEmployeeRequest(null);

        // When/Then
        assertThrows(IllegalArgumentException.class, () -> employeeService.deleteEmployee(requestWithNullId));
    }

    @Test
    void employeeService_instantiation_success() {
        // Test that the service can be created successfully
        assertThat(employeeService).isNotNull();
    }

    @Test
    void findAllEmployees_success_returnsEmployeeList() throws Exception {
        // Given
        List<Employee> employees = Arrays.asList(
                createTestEmployee("1", "John Doe", 50000, 30, "Developer"),
                createTestEmployee("2", "Jane Smith", 60000, 35, "Manager"));

        // Create JSON that exactly matches the Mock API structure
        String jsonResponse = createEmployeeListJsonResponse(employees);

        mockWebServer.enqueue(new MockResponse()
                .setBody(jsonResponse)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        // When
        List<Employee> result = employeeService.findAllEmployees().join();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("John Doe");
        assertThat(result.get(1).getName()).isEqualTo("Jane Smith");

        RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getPath()).isEqualTo("/api/v1/employee");
        assertThat(request.getMethod()).isEqualTo("GET");
    }

    @Test
    void findAllEmployeesByName_success_returnsFilteredList() throws Exception {
        // Given - Service now calls /api/v1/employee and filters client-side
        List<Employee> employees = Arrays.asList(
                createTestEmployee("1", "John Doe", 50000, 30, "Developer"),
                createTestEmployee("2", "Jane Smith", 60000, 35, "Manager"),
                createTestEmployee("3", "Johnny Cash", 70000, 40, "Lead Developer"));

        String jsonResponse = createEmployeeListJsonResponse(employees);

        mockWebServer.enqueue(new MockResponse()
                .setBody(jsonResponse)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        // When
        List<Employee> result = employeeService.findAllEmployeesByName("John").join();

        // Then - Should return employees containing "John" in their name
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Employee::getName).containsExactlyInAnyOrder("John Doe", "Johnny Cash");

        RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getPath()).isEqualTo("/api/v1/employee");
        assertThat(request.getMethod()).isEqualTo("GET");
    }

    @Test
    void findEmployeeById_success_returnsEmployee() throws Exception {
        // Given
        Employee employee = createTestEmployee("123", "Alice Brown", 75000, 28, "Senior Developer");
        ApiResponse<Employee> response = new ApiResponse<>();
        response.setData(employee);
        response.setStatus("success");

        mockWebServer.enqueue(new MockResponse()
                .setBody(createJsonResponse(response))
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        // When
        Employee result = employeeService.findEmployeeById("123").join();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Alice Brown");
        assertThat(result.getSalary()).isEqualTo(75000);

        RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getPath()).isEqualTo("/api/v1/employee/123");
        assertThat(request.getMethod()).isEqualTo("GET");
    }

    @Test
    void getHighestSalaryOfEmployees_success_returnsSalary() throws Exception {
        // Given - Service now calls /api/v1/employee and calculates highest salary client-side
        List<Employee> employees = Arrays.asList(
                createTestEmployee("1", "John Doe", 50000, 30, "Developer"),
                createTestEmployee("2", "Jane Smith", 100000, 35, "Manager"),
                createTestEmployee("3", "Bob Johnson", 75000, 40, "Senior Developer"));

        String jsonResponse = createEmployeeListJsonResponse(employees);

        mockWebServer.enqueue(new MockResponse()
                .setBody(jsonResponse)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        // When
        Integer result = employeeService.getHighestSalaryOfEmployees().join();

        // Then
        assertThat(result).isEqualTo(100000); // Jane Smith has the highest salary

        RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getPath()).isEqualTo("/api/v1/employee");
        assertThat(request.getMethod()).isEqualTo("GET");
    }

    @Test
    void getTop10HighestEarningEmployeeNames_success_returnsNames() throws Exception {
        // Given - Service now calls /api/v1/employee and calculates top 10 client-side
        List<Employee> employees = Arrays.asList(
                createTestEmployee("1", "Alice Brown", 100000, 30, "Manager"),
                createTestEmployee("2", "Bob Wilson", 95000, 35, "Senior Developer"),
                createTestEmployee("3", "Carol Davis", 90000, 28, "Developer"),
                createTestEmployee("4", "David Green", 85000, 32, "Developer"),
                createTestEmployee("5", "Eve Martinez", 80000, 29, "Analyst"));

        String jsonResponse = createEmployeeListJsonResponse(employees);

        mockWebServer.enqueue(new MockResponse()
                .setBody(jsonResponse)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        // When
        List<String> result =
                employeeService.getTop10HighestEarningEmployeeNames().join();

        // Then
        assertThat(result).hasSize(5); // All 5 employees since we have less than 10
        assertThat(result).containsExactly("Alice Brown", "Bob Wilson", "Carol Davis", "David Green", "Eve Martinez");

        RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getPath()).isEqualTo("/api/v1/employee");
        assertThat(request.getMethod()).isEqualTo("GET");
    }

    @Test
    void createEmployee_success_returnsCreatedEmployee() throws Exception {
        // Given
        CreateEmployeeRequest request = new CreateEmployeeRequest("David Green", "Analyst", 55000, 26);
        Employee createdEmployee = createTestEmployee("456", "David Green", 55000, 26, "Analyst");
        ApiResponse<Employee> response = new ApiResponse<>();
        response.setData(createdEmployee);
        response.setStatus("success");

        mockWebServer.enqueue(new MockResponse()
                .setBody(createJsonResponse(response))
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        // When
        Employee result = employeeService.createEmployee(request).join();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("David Green");
        assertThat(result.getTitle()).isEqualTo("Analyst");

        RecordedRequest recordedRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertThat(recordedRequest).isNotNull();
        assertThat(recordedRequest.getPath()).isEqualTo("/api/v1/employee");
        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        assertThat(recordedRequest.getHeader(HttpHeaders.CONTENT_TYPE)).contains("application/json");
    }

    // Helper methods for creating test data
    private Employee createTestEmployee(String id, String name, int salary, int age, String title) {
        return Employee.builder()
                .id(id)
                .name(name)
                .salary(salary)
                .age(age)
                .title(title)
                .email(Employee.getEmailFromName(name))
                .build();
    }

    private String createJsonResponse(Object object) throws JsonProcessingException {
        return objectMapper.writeValueAsString(object);
    }

    private String createEmployeeListJsonResponse(List<Employee> employees) {
        // Create JSON structure that exactly matches Mock API format
        StringBuilder json = new StringBuilder();
        json.append("{\"data\":[");

        for (int i = 0; i < employees.size(); i++) {
            Employee emp = employees.get(i);
            if (i > 0) json.append(",");
            json.append("{")
                    .append("\"id\":\"")
                    .append(emp.getId())
                    .append("\",")
                    .append("\"employee_name\":\"")
                    .append(emp.getName())
                    .append("\",")
                    .append("\"employee_salary\":")
                    .append(emp.getSalary())
                    .append(",")
                    .append("\"employee_age\":")
                    .append(emp.getAge())
                    .append(",")
                    .append("\"employee_title\":\"")
                    .append(emp.getTitle())
                    .append("\",")
                    .append("\"employee_email\":\"")
                    .append(emp.getEmail())
                    .append("\"")
                    .append("}");
        }

        json.append("],\"status\":\"success\"}");
        return json.toString();
    }
}
