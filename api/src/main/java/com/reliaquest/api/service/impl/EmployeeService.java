// api/src/main/java/com/reliaquest/api/service/impl/EmployeeServiceImpl.java
package com.reliaquest.api.service.impl;

import com.reliaquest.api.model.CreateEmployeeRequest;
import com.reliaquest.api.model.DeleteEmployeeRequest;
import com.reliaquest.api.model.Employee;
import com.reliaquest.api.service.IEmployeeService;
import com.reliaquest.api.utils.ApiResponse;
import com.reliaquest.api.utils.ErrorUtil;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@Validated
public class EmployeeService implements IEmployeeService {
    private static final Logger log = LoggerFactory.getLogger(EmployeeService.class);

    private final WebClient client;

    public EmployeeService(@NonNull WebClient.Builder builder) {
        this.client = builder.baseUrl("http://localhost:8112/api/v1/employee").build();
    }

    @Override
    @NonNull public CompletableFuture<List<Employee>> findAllEmployees() {
        log.info("Fetching all employees from Mock API");
        var type = new ParameterizedTypeReference<ApiResponse<List<Employee>>>() {};

        return client.get()
                .retrieve()
                .onStatus(
                        status -> status == HttpStatus.TOO_MANY_REQUESTS,
                        ErrorUtil.handleRateLimit("Rate limited by Mock API while fetching all employees"))
                .bodyToMono(type)
                .retryWhen(ErrorUtil.rateLimitRetry())
                .map(response -> {
                    if (response == null || response.getData() == null) {
                        return new ArrayList<Employee>();
                    }
                    List<Employee> employees = response.getData();
                    // Memory safety: Limit collection size to prevent OOM attacks
                    if (employees.size() > 10000) {
                        log.warn("Employee list size {} exceeds safety limit, truncating to 10000", employees.size());
                        return employees.subList(0, 10000);
                    }
                    return employees;
                })
                .toFuture();
    }

    @Override
    public CompletableFuture<List<Employee>> findAllEmployeesByName(
            @NotBlank @Size(min = 1, max = 100) String nameString) {
        log.info("Fetching employees with the name : {}", nameString);

        return findAllEmployees().thenApply(allEmployees -> allEmployees.stream()
                .filter(employee -> employee.getName() != null
                        && employee.getName().toLowerCase().contains(nameString.toLowerCase()))
                .toList());
    }

    @Override
    public CompletableFuture<Employee> findEmployeeById(@NotBlank @Pattern(regexp = "^[a-zA-Z0-9-]+$") String id) {
        log.info("Fetching employee with ID: {}", id);
        var type = new ParameterizedTypeReference<ApiResponse<Employee>>() {};

        return client.get()
                .uri("/{id}", id)
                .retrieve()
                .onStatus(
                        status -> status == HttpStatus.NOT_FOUND,
                        ErrorUtil.handleNotFound("Employee with ID " + id + " not found"))
                .onStatus(
                        status -> status == HttpStatus.TOO_MANY_REQUESTS,
                        ErrorUtil.handleRateLimit("Rate limited during employee lookup for ID: " + id))
                .bodyToMono(type)
                .retryWhen(ErrorUtil.rateLimitRetry())
                .map(response -> response != null ? response.getData() : null)
                .toFuture();
    }

    @Override
    public CompletableFuture<Integer> getHighestSalaryOfEmployees() {
        log.info("Fetching highest salary of employees");

        return findAllEmployees().thenApply(allEmployees -> allEmployees.stream()
                .mapToInt(Employee::getSalary)
                .max()
                .orElseThrow(() -> new IllegalStateException("No employees found")));
    }

    @Override
    public CompletableFuture<List<String>> getTop10HighestEarningEmployeeNames() {
        log.info("Fetching top 10 highest earning employee names");

        return findAllEmployees().thenApply(allEmployees -> allEmployees.stream()
                .sorted((e1, e2) -> Integer.compare(e2.getSalary(), e1.getSalary())) // Sort descending by salary
                .limit(10)
                .map(Employee::getName)
                .toList());
    }

    @Override
    public CompletableFuture<Employee> createEmployee(@NotNull @RequestBody CreateEmployeeRequest employeeInput) {
        log.info("Creating new employee: {}", employeeInput);

        Employee employee = Employee.builder()
                .name(employeeInput.getName())
                .salary(employeeInput.getSalary())
                .age(employeeInput.getAge())
                .title(employeeInput.getTitle())
                .email(employeeInput.nameToEmail())
                .build();

        return client.post()
                .bodyValue(employee)
                .retrieve()
                .onStatus(
                        status -> status == HttpStatus.BAD_REQUEST,
                        ErrorUtil.handleBadRequest("Failed to create employee: invalid input data"))
                .onStatus(
                        status -> status == HttpStatus.INTERNAL_SERVER_ERROR,
                        ErrorUtil.handleError("Internal server error while creating employee"))
                .onStatus(
                        status -> status == HttpStatus.TOO_MANY_REQUESTS,
                        ErrorUtil.handleRateLimit("Rate limited during employee creation"))
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<Employee>>() {})
                .retryWhen(ErrorUtil.rateLimitRetry())
                .map(response -> response != null ? response.getData() : null)
                .toFuture();
    }

    @Override
    public CompletableFuture<String> deleteEmployee(@NotNull @RequestBody DeleteEmployeeRequest deleteRequest) {
        log.info("Deleting employee: {}", deleteRequest);
        if (deleteRequest == null || deleteRequest.getId() == null) {
            throw new IllegalArgumentException("Delete request and employee Id must not be null");
        }

        // Race condition mitigation: Get employee info and attempt delete in single operation
        // If employee doesn't exist, handle gracefully instead of pre-checking
        return findEmployeeById(deleteRequest.getId())
                .thenCompose(employee -> {
                    if (employee == null) {
                        throw new IllegalArgumentException("Employee with ID " + deleteRequest.getId() + " not found");
                    }

                    // Store employee name before deletion attempt
                    String employeeName = employee.getName();

                    return client.method(org.springframework.http.HttpMethod.DELETE)
                            .uri("/{name}", employeeName)
                            .bodyValue(Map.of("name", employeeName))
                            .retrieve()
                            .onStatus(status -> status == HttpStatus.NOT_FOUND, clientResponse -> {
                                // Employee was deleted between our GET and DELETE calls
                                // Return the name anyway since deletion was the intent
                                log.warn("Employee {} was deleted by another request during deletion", employeeName);
                                return clientResponse
                                        .createException()
                                        .flatMap(ex -> Mono.error(new IllegalArgumentException(
                                                "Employee with ID " + deleteRequest.getId() + " was already deleted")));
                            })
                            .onStatus(
                                    status -> status == HttpStatus.BAD_REQUEST,
                                    ErrorUtil.handleBadRequest(
                                            "Invalid delete request for employee with ID " + deleteRequest.getId()))
                            .onStatus(
                                    status -> status == HttpStatus.INTERNAL_SERVER_ERROR,
                                    ErrorUtil.handleError("Internal server error while deleting employee with ID "
                                            + deleteRequest.getId()))
                            .onStatus(
                                    status -> status == HttpStatus.TOO_MANY_REQUESTS,
                                    ErrorUtil.handleRateLimit("Rate limited during employee deletion: " + employeeName))
                            .bodyToMono(new ParameterizedTypeReference<ApiResponse<Boolean>>() {})
                            .retryWhen(ErrorUtil.rateLimitRetry())
                            .map(response -> employeeName)
                            .onErrorReturn(
                                    IllegalArgumentException.class, employeeName) // Return name even if already deleted
                            .toFuture();
                })
                .exceptionally(throwable -> {
                    // Handle race condition where employee is deleted between GET and DELETE
                    if (throwable.getCause() instanceof IllegalArgumentException
                            && throwable.getMessage().contains("already deleted")) {
                        // Extract employee name from the error and return it
                        log.info("Handling race condition: employee was already deleted");
                        throw new IllegalArgumentException("Employee with ID " + deleteRequest.getId() + " not found");
                    }
                    throw new RuntimeException(throwable);
                });
    }
}
