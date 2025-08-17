// api/src/main/java/com/reliaquest/api/controller/EmployeeController.java
package com.reliaquest.api.controller;

import com.reliaquest.api.model.CreateEmployeeRequest;
import com.reliaquest.api.model.DeleteEmployeeRequest;
import com.reliaquest.api.model.Employee;
import com.reliaquest.api.service.IEmployeeService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.concurrent.CompletionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/api/v1/employee")
public class EmployeeController implements IEmployeeController<Employee, CreateEmployeeRequest> {

    private final IEmployeeService employeeService;

    public EmployeeController(IEmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @Override
    public ResponseEntity<List<Employee>> getAllEmployees() {
        return ResponseEntity.ok(employeeService.findAllEmployees().join());
    }

    @Override
    public ResponseEntity<List<Employee>> getEmployeesByNameSearch(@PathVariable String searchString) {
        return ResponseEntity.ok(
                employeeService.findAllEmployeesByName(searchString).join());
    }

    @Override
    public ResponseEntity<Employee> getEmployeeById(@PathVariable String id) {
        Employee employee = employeeService.findEmployeeById(id).join();
        if (employee == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found with id: " + id);
        }
        return ResponseEntity.ok(employee);
    }

    @Override
    public ResponseEntity<Integer> getHighestSalaryOfEmployees() {
        return ResponseEntity.ok(employeeService.getHighestSalaryOfEmployees().join());
    }

    @Override
    public ResponseEntity<List<String>> getTopTenHighestEarningEmployeeNames() {
        return ResponseEntity.ok(
                employeeService.getTop10HighestEarningEmployeeNames().join());
    }

    @Override
    public ResponseEntity<Employee> createEmployee(@Valid @RequestBody CreateEmployeeRequest employeeInput) {
        try {
            Employee createdEmployee =
                    employeeService.createEmployee(employeeInput).join();
            return ResponseEntity.status(HttpStatus.CREATED).body(createdEmployee);
        } catch (WebClientResponseException.InternalServerError e) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "Employee creation service is temporarily unavailable");
        } catch (WebClientResponseException.BadRequest e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid employee data provided");
        } catch (WebClientResponseException.TooManyRequests e) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS, "Service is rate limited, please try again later");
        }
    }

    @Override
    public ResponseEntity<String> deleteEmployeeById(@PathVariable String id) {
        try {
            String deletedEmployeeName = employeeService
                    .deleteEmployee(new DeleteEmployeeRequest(id))
                    .join();
            return ResponseEntity.ok(deletedEmployeeName);
        } catch (CompletionException e) {
            // Unwrap the CompletionException to get the underlying cause
            Throwable cause = e.getCause();
            if (cause instanceof IllegalArgumentException iae) {
                if (iae.getMessage().contains("not found")) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, iae.getMessage());
                }
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, iae.getMessage());
            }
            if (cause instanceof WebClientResponseException.InternalServerError) {
                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE, "Employee deletion service is temporarily unavailable");
            }
            if (cause instanceof WebClientResponseException.NotFound) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee with ID " + id + " not found");
            }
            if (cause instanceof WebClientResponseException.BadRequest) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Invalid delete request for employee with ID " + id);
            }
            if (cause instanceof WebClientResponseException.TooManyRequests) {
                throw new ResponseStatusException(
                        HttpStatus.TOO_MANY_REQUESTS, "Service is rate limited, please try again later");
            }
            // Re-throw if we don't know how to handle the underlying exception
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error during employee deletion");
        } catch (WebClientResponseException.InternalServerError e) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "Employee deletion service is temporarily unavailable");
        } catch (WebClientResponseException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee with ID " + id + " not found");
        } catch (WebClientResponseException.BadRequest e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid delete request for employee with ID " + id);
        } catch (WebClientResponseException.TooManyRequests e) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS, "Service is rate limited, please try again later");
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("not found")) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
