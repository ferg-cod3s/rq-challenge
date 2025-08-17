// Java
package com.reliaquest.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reliaquest.api.model.CreateEmployeeRequest;
import com.reliaquest.api.model.DeleteEmployeeRequest;
import com.reliaquest.api.model.Employee;
import com.reliaquest.api.service.impl.EmployeeService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = EmployeeController.class)
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper; // used to serialize request bodies

    @MockBean
    private EmployeeService employeeService;

    @Test
    void getAllEmployees_returnsOkWithList() throws Exception {
        var e1 = new Employee(
                "4a3a170b-22cd-4ac2-aad1-9bb5b34a1507",
                "Tiger Nixon",
                320800,
                61,
                "Vice Chair Executive Principal of Chief Operations Implementation Specialist",
                "tnixon@company.com");
        var e2 = new Employee(
                "5255f1a5-f9f7-4be5-829a-134bde088d17",
                "Bill Bob",
                89750,
                24,
                "Documentation Engineer",
                "billBob@company.com");
        when(employeeService.findAllEmployees())
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(List.of(e1, e2)));

        mockMvc.perform(get("/api/v1/employee").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0][\"employee_name\"]").value("Tiger Nixon"))
                .andExpect(jsonPath("$[0][\"employee_salary\"]").value(320800))
                .andExpect(jsonPath("$[1][\"employee_name\"]").value("Bill Bob"));
    }

    @Test
    void getAllEmployees_returnsOkWithEmptyList() throws Exception {
        when(employeeService.findAllEmployees())
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(List.of()));

        mockMvc.perform(get("/api/v1/employee").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void getEmployeeById_found_returnsOk() throws Exception {
        var id = "4a3a170b-22cd-4ac2-aad1-9bb5b34a1507";
        var e = new Employee(
                id,
                "Tiger Nixon",
                320800,
                61,
                "Vice Chair Executive Principal of Chief Operations Implementation Specialist",
                "tnixon@company.com");

        when(employeeService.findEmployeeById(eq(id)))
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(e));

        mockMvc.perform(get("/api/v1/employee/{id}", id).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employee_name").value("Tiger Nixon"))
                .andExpect(jsonPath("$.employee_salary").value(320800));
    }

    @Test
    void getEmployeeById_notFound_returnsNotFound() throws Exception {
        var id = "non-existent-id";
        when(employeeService.findEmployeeById(eq(id)))
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));

        mockMvc.perform(get("/api/v1/employee/{id}", id).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void createEmployee_valid_returnsCreated() throws Exception {
        var request = new CreateEmployeeRequest("New Employee", "Junior Developer", 50000, 30);
        var created = new Employee(
                "generated-id-123",
                request.getName(),
                request.getSalary(),
                request.getAge(),
                request.getTitle(),
                request.nameToEmail());

        when(employeeService.createEmployee(any(CreateEmployeeRequest.class)))
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(created));

        mockMvc.perform(post("/api/v1/employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("generated-id-123"))
                .andExpect(jsonPath("$.employee_name").value("New Employee"));
    }

    @Test
    void deleteEmployee_existing_returnsNoContent() throws Exception {
        var id = "to-delete-id-1";
        when(employeeService.deleteEmployee(any(DeleteEmployeeRequest.class)))
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture("Employee Name"));

        mockMvc.perform(delete("/api/v1/employee/{id}", id))
                .andExpect(status().isOk())
                .andExpect(content().string("Employee Name"));
    }

    @Test
    void createEmployee_invalidName_empty_returnsBadRequest() throws Exception {
        var invalidRequest = new CreateEmployeeRequest("", "Valid Title", 50000, 25); // Name too short

        mockMvc.perform(post("/api/v1/employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createEmployee_invalidName_123_returnsBadRequest() throws Exception {
        var invalidRequest = new CreateEmployeeRequest("123", "Valid Title", 50000, 25); // Name too short

        mockMvc.perform(post("/api/v1/employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}
