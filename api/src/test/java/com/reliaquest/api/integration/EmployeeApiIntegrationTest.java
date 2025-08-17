package com.reliaquest.api.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reliaquest.api.model.CreateEmployeeRequest;
import com.reliaquest.api.model.DeleteEmployeeRequest;
import com.reliaquest.api.model.Employee;
import com.reliaquest.api.service.impl.EmployeeService;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class EmployeeApiIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmployeeService employeeService;

    private String baseUrl;
    private Employee testEmployee1;
    private Employee testEmployee2;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/v1/employee";

        testEmployee1 = Employee.builder()
                .id("4a3a170b-22cd-4ac2-aad1-9bb5b34a1507")
                .name("Tiger Nixon")
                .salary(320800)
                .age(61)
                .title("Vice Chair Executive Principal")
                .email("tnixon@company.com")
                .build();

        testEmployee2 = Employee.builder()
                .id("5255f1a5-f9f7-4be5-829a-134bde088d17")
                .name("Bill Bob")
                .salary(89750)
                .age(24)
                .title("Documentation Engineer")
                .email("billbob@company.com")
                .build();
    }

    @Test
    void testGetAllEmployees_returnsCorrectJsonFormat() throws Exception {
        // Given
        when(employeeService.findAllEmployees())
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(
                        Arrays.asList(testEmployee1, testEmployee2)));

        // When
        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl, String.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode jsonArray = objectMapper.readTree(response.getBody());
        assertThat(jsonArray.isArray()).isTrue();
        assertThat(jsonArray.size()).isEqualTo(2);

        JsonNode firstEmployee = jsonArray.get(0);
        assertThat(firstEmployee.get("id").asText()).isEqualTo("4a3a170b-22cd-4ac2-aad1-9bb5b34a1507");
        assertThat(firstEmployee.get("employee_name").asText()).isEqualTo("Tiger Nixon");
        assertThat(firstEmployee.get("employee_salary").asInt()).isEqualTo(320800);
        assertThat(firstEmployee.get("employee_age").asInt()).isEqualTo(61);
        assertThat(firstEmployee.get("employee_title").asText()).isEqualTo("Vice Chair Executive Principal");
        assertThat(firstEmployee.get("employee_email").asText()).isEqualTo("tnixon@company.com");
    }

    @Test
    void testGetEmployeesByNameSearch_returnsFilteredResults() throws Exception {
        // Given
        String searchTerm = "Tiger";
        when(employeeService.findAllEmployeesByName(searchTerm))
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(Arrays.asList(testEmployee1)));

        // When
        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl + "/search/" + searchTerm, String.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode jsonArray = objectMapper.readTree(response.getBody());
        assertThat(jsonArray.isArray()).isTrue();
        assertThat(jsonArray.size()).isEqualTo(1);
        assertThat(jsonArray.get(0).get("employee_name").asText()).isEqualTo("Tiger Nixon");
    }

    @Test
    void testGetEmployeeById_returnsCorrectFormat() throws Exception {
        // Given
        String employeeId = "4a3a170b-22cd-4ac2-aad1-9bb5b34a1507";
        when(employeeService.findEmployeeById(employeeId))
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(testEmployee1));

        // When
        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl + "/" + employeeId, String.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode employee = objectMapper.readTree(response.getBody());
        assertThat(employee.get("id").asText()).isEqualTo(employeeId);
        assertThat(employee.get("employee_name").asText()).isEqualTo("Tiger Nixon");
        assertThat(employee.get("employee_salary").asInt()).isEqualTo(320800);
    }

    @Test
    void testGetEmployeeById_returns404WhenNotFound() {
        // Given
        String nonExistentId = "non-existent-id";
        when(employeeService.findEmployeeById(nonExistentId))
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));

        // When
        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl + "/" + nonExistentId, String.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void testGetHighestSalaryOfEmployees_returnsInteger() {
        // Given
        when(employeeService.getHighestSalaryOfEmployees())
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(320800));

        // When
        ResponseEntity<Integer> response = restTemplate.getForEntity(baseUrl + "/highestSalary", Integer.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(320800);
    }

    @Test
    void testGetTopTenHighestEarningEmployeeNames_returnsCorrectFormat() {
        // Given - This tests the interface contract (List<String>)
        List<String> topNames = Arrays.asList("Tiger Nixon", "Bill Bob");
        when(employeeService.getTop10HighestEarningEmployeeNames())
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(topNames));

        // When
        ResponseEntity<String[]> response =
                restTemplate.getForEntity(baseUrl + "/topTenHighestEarningEmployeeNames", String[].class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody()).containsExactly("Tiger Nixon", "Bill Bob");
    }

    @Test
    void testCreateEmployee_returnsCreatedWithCorrectFormat() throws Exception {
        // Given
        CreateEmployeeRequest request = new CreateEmployeeRequest("New Employee", "Junior Developer", 50000, 30);
        Employee createdEmployee = Employee.builder()
                .id("generated-id-123")
                .name("New Employee")
                .salary(50000)
                .age(30)
                .title("Junior Developer")
                .email("newe@company.com")
                .build();

        when(employeeService.createEmployee(any(CreateEmployeeRequest.class)))
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(createdEmployee));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateEmployeeRequest> entity = new HttpEntity<>(request, headers);

        // When
        ResponseEntity<String> response = restTemplate.postForEntity(baseUrl, entity, String.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        JsonNode employee = objectMapper.readTree(response.getBody());
        assertThat(employee.get("id").asText()).isEqualTo("generated-id-123");
        assertThat(employee.get("employee_name").asText()).isEqualTo("New Employee");
        assertThat(employee.get("employee_salary").asInt()).isEqualTo(50000);
        assertThat(employee.get("employee_age").asInt()).isEqualTo(30);
        assertThat(employee.get("employee_title").asText()).isEqualTo("Junior Developer");
        assertThat(employee.get("employee_email").asText()).isEqualTo("newe@company.com");
    }

    @Test
    void testCreateEmployee_returns400ForInvalidData() {
        // Given - Invalid request (negative salary)
        CreateEmployeeRequest invalidRequest = new CreateEmployeeRequest("", "Junior Developer", -1, 30);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateEmployeeRequest> entity = new HttpEntity<>(invalidRequest, headers);

        // When
        ResponseEntity<String> response = restTemplate.postForEntity(baseUrl, entity, String.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void testDeleteEmployeeById_returnsEmployeeName() {
        // Given
        String employeeId = "4a3a170b-22cd-4ac2-aad1-9bb5b34a1507";
        String employeeName = "Tiger Nixon";
        when(employeeService.deleteEmployee(any(DeleteEmployeeRequest.class)))
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(employeeName));

        // When
        ResponseEntity<String> response =
                restTemplate.exchange(baseUrl + "/" + employeeId, HttpMethod.DELETE, null, String.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(employeeName);
    }

    @Test
    void testDeleteEmployeeById_returns404WhenNotFound() {
        // Given
        String nonExistentId = "non-existent-id";
        when(employeeService.deleteEmployee(any(DeleteEmployeeRequest.class))).thenAnswer(invocation -> {
            CompletableFuture<String> future = new CompletableFuture<>();
            future.completeExceptionally(
                    new IllegalArgumentException("Employee with ID " + nonExistentId + " not found"));
            return future;
        });

        // When
        ResponseEntity<String> response =
                restTemplate.exchange(baseUrl + "/" + nonExistentId, HttpMethod.DELETE, null, String.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
