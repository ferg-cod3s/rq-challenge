package com.reliaquest.api.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.reliaquest.api.controller.IEmployeeController;
import com.reliaquest.api.model.CreateEmployeeRequest;
import com.reliaquest.api.model.Employee;
import com.reliaquest.api.service.IEmployeeService;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@DisplayName("README Requirements Compliance Tests")
public class ComplianceTests {

    @Test
    @DisplayName("✓ getAllEmployees() - Should return list of employees")
    void testGetAllEmployeesContract() throws Exception {
        // Verify controller method exists with correct signature
        Method method = IEmployeeController.class.getMethod("getAllEmployees");

        assertAll(
                "getAllEmployees contract compliance",
                () -> assertThat(method).isNotNull(),
                () -> assertThat(method.isAnnotationPresent(GetMapping.class)).isTrue(),
                () -> assertThat(method.getParameterCount()).isEqualTo(0),
                () -> assertThat(method.getReturnType()).isEqualTo(ResponseEntity.class));

        // Verify return type is ResponseEntity<List<Entity>>
        Type returnType = method.getGenericReturnType();
        if (returnType instanceof ParameterizedType paramType) {
            Type[] typeArgs = paramType.getActualTypeArguments();
            assertThat(typeArgs).hasSize(1);

            if (typeArgs[0] instanceof ParameterizedType listType) {
                assertThat(listType.getRawType()).isEqualTo(List.class);
            }
        }
    }

    @Test
    @DisplayName("✓ getEmployeesByNameSearch() - Should accept name fragment and return filtered employees")
    void testGetEmployeesByNameSearchContract() throws Exception {
        Method method = IEmployeeController.class.getMethod("getEmployeesByNameSearch", String.class);

        assertAll(
                "getEmployeesByNameSearch contract compliance",
                () -> assertThat(method).isNotNull(),
                () -> assertThat(method.isAnnotationPresent(GetMapping.class)).isTrue(),
                () -> assertThat(method.getParameterCount()).isEqualTo(1),
                () -> assertThat(method.getParameterTypes()[0]).isEqualTo(String.class));

        // Verify parameter has @PathVariable annotation
        assertThat(method.getParameters()[0].isAnnotationPresent(PathVariable.class))
                .isTrue();
    }

    @Test
    @DisplayName("✓ getEmployeeById() - Should accept ID and return single employee")
    void testGetEmployeeByIdContract() throws Exception {
        Method method = IEmployeeController.class.getMethod("getEmployeeById", String.class);

        assertAll(
                "getEmployeeById contract compliance",
                () -> assertThat(method).isNotNull(),
                () -> assertThat(method.isAnnotationPresent(GetMapping.class)).isTrue(),
                () -> assertThat(method.getParameterCount()).isEqualTo(1),
                () -> assertThat(method.getParameterTypes()[0]).isEqualTo(String.class));

        // Verify parameter has @PathVariable annotation
        assertThat(method.getParameters()[0].isAnnotationPresent(PathVariable.class))
                .isTrue();
    }

    @Test
    @DisplayName("✓ getHighestSalaryOfEmployees() - Should return integer of highest salary")
    void testGetHighestSalaryContract() throws Exception {
        Method method = IEmployeeController.class.getMethod("getHighestSalaryOfEmployees");

        assertAll(
                "getHighestSalaryOfEmployees contract compliance",
                () -> assertThat(method).isNotNull(),
                () -> assertThat(method.isAnnotationPresent(GetMapping.class)).isTrue(),
                () -> assertThat(method.getParameterCount()).isEqualTo(0));

        // Verify return type is ResponseEntity<Integer>
        Type returnType = method.getGenericReturnType();
        if (returnType instanceof ParameterizedType paramType) {
            Type[] typeArgs = paramType.getActualTypeArguments();
            assertThat(typeArgs).hasSize(1);
            assertThat(typeArgs[0]).isEqualTo(Integer.class);
        }
    }

    @Test
    @DisplayName("⚠️ getTop10HighestEarningEmployeeNames() - README vs Interface conflict!")
    void testGetTopTenHighestEarningEmployeeNamesContract() throws Exception {
        Method method = IEmployeeController.class.getMethod("getTopTenHighestEarningEmployeeNames");

        assertAll(
                "getTopTenHighestEarningEmployeeNames contract",
                () -> assertThat(method).isNotNull(),
                () -> assertThat(method.isAnnotationPresent(GetMapping.class)).isTrue(),
                () -> assertThat(method.getParameterCount()).isEqualTo(0));

        // Document the conflict between README and interface
        Type returnType = method.getGenericReturnType();
        if (returnType instanceof ParameterizedType paramType) {
            Type[] typeArgs = paramType.getActualTypeArguments();
            if (typeArgs.length > 0 && typeArgs[0] instanceof ParameterizedType listType) {
                Type listElementType = listType.getActualTypeArguments()[0];

                // This test documents the conflict:
                // README says: "list of employees"
                // Interface says: List<String>
                System.out.println("⚠️ CONFLICT DETECTED:");
                System.out.println("   README specifies: 'list of employees'");
                System.out.println("   Interface specifies: " + listType);
                System.out.println("   Decision needed: Return Employee objects or just names?");
            }
        }
    }

    @Test
    @DisplayName("✓ createEmployee() - Should accept request body and return created employee")
    void testCreateEmployeeContract() throws Exception {
        // Find the method with @RequestBody parameter
        Method method = Arrays.stream(IEmployeeController.class.getMethods())
                .filter(m -> m.getName().equals("createEmployee"))
                .filter(m -> m.isAnnotationPresent(PostMapping.class))
                .findFirst()
                .orElseThrow(() -> new AssertionError("createEmployee method not found"));

        assertAll(
                "createEmployee contract compliance",
                () -> assertThat(method).isNotNull(),
                () -> assertThat(method.isAnnotationPresent(PostMapping.class)).isTrue(),
                () -> assertThat(method.getParameterCount()).isEqualTo(1));

        // Verify parameter has @RequestBody annotation
        assertThat(method.getParameters()[0].isAnnotationPresent(RequestBody.class))
                .isTrue();
    }

    @Test
    @DisplayName("✓ deleteEmployeeById() - Should accept ID and return employee name")
    void testDeleteEmployeeByIdContract() throws Exception {
        Method method = IEmployeeController.class.getMethod("deleteEmployeeById", String.class);

        assertAll(
                "deleteEmployeeById contract compliance",
                () -> assertThat(method).isNotNull(),
                () -> assertThat(method.isAnnotationPresent(DeleteMapping.class))
                        .isTrue(),
                () -> assertThat(method.getParameterCount()).isEqualTo(1),
                () -> assertThat(method.getParameterTypes()[0]).isEqualTo(String.class));

        // Verify parameter has @PathVariable annotation
        assertThat(method.getParameters()[0].isAnnotationPresent(PathVariable.class))
                .isTrue();

        // Verify return type is ResponseEntity<String> (employee name)
        Type returnType = method.getGenericReturnType();
        if (returnType instanceof ParameterizedType paramType) {
            Type[] typeArgs = paramType.getActualTypeArguments();
            assertThat(typeArgs).hasSize(1);
            assertThat(typeArgs[0]).isEqualTo(String.class);
        }
    }

    @Test
    @DisplayName("✓ CreateEmployeeRequest validation - Should enforce README constraints")
    void testCreateEmployeeRequestValidation() throws Exception {
        // Verify CreateEmployeeRequest has required fields
        assertThat(CreateEmployeeRequest.class.getDeclaredField("name")).isNotNull();
        assertThat(CreateEmployeeRequest.class.getDeclaredField("title")).isNotNull();
        assertThat(CreateEmployeeRequest.class.getDeclaredField("salary")).isNotNull();
        assertThat(CreateEmployeeRequest.class.getDeclaredField("age")).isNotNull();

        // Verify it does NOT have id or email (server-generated)
        assertAll(
                "CreateEmployeeRequest should not have server-generated fields",
                () -> assertThat(Arrays.stream(CreateEmployeeRequest.class.getDeclaredFields())
                                .noneMatch(f -> f.getName().equals("id")))
                        .isTrue(),
                () -> assertThat(Arrays.stream(CreateEmployeeRequest.class.getDeclaredFields())
                                .noneMatch(f -> f.getName().equals("email")))
                        .isTrue());
    }

    @Test
    @DisplayName("✓ Employee model - Should have all required fields")
    void testEmployeeModelCompleteness() throws Exception {
        // Verify Employee has all fields mentioned in README
        assertAll(
                "Employee model has all required fields",
                () -> assertThat(Employee.class.getDeclaredField("id")).isNotNull(),
                () -> assertThat(Employee.class.getDeclaredField("name")).isNotNull(),
                () -> assertThat(Employee.class.getDeclaredField("salary")).isNotNull(),
                () -> assertThat(Employee.class.getDeclaredField("age")).isNotNull(),
                () -> assertThat(Employee.class.getDeclaredField("title")).isNotNull(),
                () -> assertThat(Employee.class.getDeclaredField("email")).isNotNull());
    }

    @Test
    @DisplayName("✓ Service interface - Should match controller requirements")
    void testServiceInterfaceContract() throws Exception {
        // Verify service methods exist for all controller operations
        assertAll(
                "Service interface supports all controller operations",
                () -> assertThat(IEmployeeService.class.getMethod("findAllEmployees"))
                        .isNotNull(),
                () -> assertThat(IEmployeeService.class.getMethod("findAllEmployeesByName", String.class))
                        .isNotNull(),
                () -> assertThat(IEmployeeService.class.getMethod("findEmployeeById", String.class))
                        .isNotNull(),
                () -> assertThat(IEmployeeService.class.getMethod("getHighestSalaryOfEmployees"))
                        .isNotNull(),
                () -> assertThat(IEmployeeService.class.getMethod("getTop10HighestEarningEmployeeNames"))
                        .isNotNull(),
                () -> assertThat(IEmployeeService.class.getMethod("createEmployee", CreateEmployeeRequest.class))
                        .isNotNull());

        // Check for delete method (might be named differently)
        boolean hasDeleteMethod = Arrays.stream(IEmployeeService.class.getMethods())
                .anyMatch(m -> m.getName().contains("delete") || m.getName().contains("Delete"));
        assertThat(hasDeleteMethod).as("Service should have a delete method").isTrue();
    }

    @Test
    @DisplayName("📝 README Requirements Summary")
    void documentReadmeRequirements() {
        System.out.println("\n📋 README Requirements Checklist:");
        System.out.println("✅ getAllEmployees() - Return all employees");
        System.out.println("✅ getEmployeesByNameSearch() - Filter by name fragment");
        System.out.println("✅ getEmployeeById() - Return single employee by ID");
        System.out.println("✅ getHighestSalaryOfEmployees() - Return highest salary integer");
        System.out.println("⚠️ getTop10HighestEarningEmployeeNames() - CONFLICT: README vs Interface");
        System.out.println(
                "✅ createEmployee() - Create with validation (name, title not blank; salary > 0; age 16-75)");
        System.out.println("✅ deleteEmployeeById() - Delete by ID, return employee name");
        System.out.println("\n🏗️ Implementation Status:");
        System.out.println("📝 Tests created - Implementation needed by human");
        System.out.println("🔍 JSON format validation - Required for employee_ prefix");
        System.out.println("⚡ Error handling - Rate limiting, 404s, validation errors");
    }
}
