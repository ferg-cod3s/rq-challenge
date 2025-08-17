// api/src/main/java/com/reliaquest/api/service/EmployeeService.java
package com.reliaquest.api.service;

import com.reliaquest.api.model.CreateEmployeeRequest;
import com.reliaquest.api.model.DeleteEmployeeRequest;
import com.reliaquest.api.model.Employee;
import com.reliaquest.api.utils.RegexUtil;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface IEmployeeService {
    CompletableFuture<List<Employee>> findAllEmployees();

    CompletableFuture<List<Employee>> findAllEmployeesByName(@NotBlank @Size(min = 1, max = 100) String name);

    CompletableFuture<Employee> findEmployeeById(@NotBlank @Pattern(regexp = RegexUtil.ALPHANUMERIC_HYPHEN_REGEX) String id);

    CompletableFuture<Integer> getHighestSalaryOfEmployees();

    CompletableFuture<List<String>> getTop10HighestEarningEmployeeNames();

    CompletableFuture<Employee> createEmployee(@NotNull CreateEmployeeRequest employee);

    CompletableFuture<String> deleteEmployee(@NotNull DeleteEmployeeRequest deleteEmployeeRequest);
}
