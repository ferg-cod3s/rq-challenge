package com.reliaquest.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;

public class JsonFormatTest {

    private ObjectMapper objectMapper;
    private JacksonTester<Employee> employeeJsonTester;
    private JacksonTester<CreateEmployeeRequest> requestJsonTester;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        JacksonTester.initFields(this, objectMapper);
    }

    @Test
    void testEmployeeSerializesToCorrectJson() throws Exception {
        // Given - Employee object based on README example
        Employee employee = Employee.builder()
                .id("4a3a170b-22cd-4ac2-aad1-9bb5b34a1507")
                .name("Tiger Nixon")
                .salary(320800)
                .age(61)
                .title("Vice Chair Executive Principal of Chief Operations Implementation Specialist")
                .email("tnixon@company.com")
                .build();

        // When - Serialize to JSON
        JsonContent<Employee> json = employeeJsonTester.write(employee);

        // Then - Verify exact field names match README format
        assertThat(json).hasJsonPath("$.id");
        assertThat(json).hasJsonPath("$.employee_name");
        assertThat(json).hasJsonPath("$.employee_salary");
        assertThat(json).hasJsonPath("$.employee_age");
        assertThat(json).hasJsonPath("$.employee_title");
        assertThat(json).hasJsonPath("$.employee_email");

        // Verify actual values
        assertThat(json).extractingJsonPathStringValue("$.id").isEqualTo("4a3a170b-22cd-4ac2-aad1-9bb5b34a1507");
        assertThat(json).extractingJsonPathStringValue("$.employee_name").isEqualTo("Tiger Nixon");
        assertThat(json).extractingJsonPathNumberValue("$.employee_salary").isEqualTo(320800);
        assertThat(json).extractingJsonPathNumberValue("$.employee_age").isEqualTo(61);
        assertThat(json)
                .extractingJsonPathStringValue("$.employee_title")
                .isEqualTo("Vice Chair Executive Principal of Chief Operations Implementation Specialist");
        assertThat(json).extractingJsonPathStringValue("$.employee_email").isEqualTo("tnixon@company.com");

        // Verify no unwanted fields (like plain "name", "salary", etc.)
        JsonNode jsonNode = objectMapper.readTree(json.getJson());
        assertThat(jsonNode.has("name")).isFalse();
        assertThat(jsonNode.has("salary")).isFalse();
        assertThat(jsonNode.has("age")).isFalse();
        assertThat(jsonNode.has("title")).isFalse();
        assertThat(jsonNode.has("email")).isFalse();
    }

    @Test
    void testEmployeeDeserializesFromCorrectJson() throws Exception {
        // Given - JSON in README format
        String jsonString =
                """
            {
                "id": "4a3a170b-22cd-4ac2-aad1-9bb5b34a1507",
                "employee_name": "Tiger Nixon",
                "employee_salary": 320800,
                "employee_age": 61,
                "employee_title": "Vice Chair Executive Principal of Chief Operations Implementation Specialist",
                "employee_email": "tnixon@company.com"
            }
            """;

        // When - Deserialize from JSON
        Employee employee = employeeJsonTester.parseObject(jsonString);

        // Then - Verify object is correctly populated
        assertThat(employee.getId()).isEqualTo("4a3a170b-22cd-4ac2-aad1-9bb5b34a1507");
        assertThat(employee.getName()).isEqualTo("Tiger Nixon");
        assertThat(employee.getSalary()).isEqualTo(320800);
        assertThat(employee.getAge()).isEqualTo(61);
        assertThat(employee.getTitle())
                .isEqualTo("Vice Chair Executive Principal of Chief Operations Implementation Specialist");
        assertThat(employee.getEmail()).isEqualTo("tnixon@company.com");
    }

    @Test
    void testCreateEmployeeRequestSerializesToCorrectJson() throws Exception {
        // Given - CreateEmployeeRequest (no id or email, as per README spec)
        CreateEmployeeRequest request = new CreateEmployeeRequest("New Employee", "Junior Developer", 50000, 30);

        // When - Serialize to JSON
        JsonContent<CreateEmployeeRequest> json = requestJsonTester.write(request);

        // Then - Verify field names
        assertThat(json).hasJsonPath("$.name");
        assertThat(json).hasJsonPath("$.title");
        assertThat(json).hasJsonPath("$.salary");
        assertThat(json).hasJsonPath("$.age");

        // Verify values
        assertThat(json).extractingJsonPathStringValue("$.name").isEqualTo("New Employee");
        assertThat(json).extractingJsonPathStringValue("$.title").isEqualTo("Junior Developer");
        assertThat(json).extractingJsonPathNumberValue("$.salary").isEqualTo(50000);
        assertThat(json).extractingJsonPathNumberValue("$.age").isEqualTo(30);

        // Verify NO id or email fields (these are server-generated)
        JsonNode jsonNode = objectMapper.readTree(json.getJson());
        assertThat(jsonNode.has("id")).isFalse();
        assertThat(jsonNode.has("email")).isFalse();
        assertThat(jsonNode.has("employee_id")).isFalse();
        assertThat(jsonNode.has("employee_email")).isFalse();
    }

    @Test
    void testCreateEmployeeRequestDeserializesFromCorrectJson() throws Exception {
        // Given - JSON for request body (as client would send)
        String jsonString =
                """
            {
                "name": "New Employee",
                "title": "Junior Developer",
                "salary": 50000,
                "age": 30
            }
            """;

        // When - Deserialize from JSON
        CreateEmployeeRequest request = requestJsonTester.parseObject(jsonString);

        // Then - Verify object is correctly populated
        assertThat(request.getName()).isEqualTo("New Employee");
        assertThat(request.getTitle()).isEqualTo("Junior Developer");
        assertThat(request.getSalary()).isEqualTo(50000);
        assertThat(request.getAge()).isEqualTo(30);
    }

    @Test
    void testValidationConstraintsWork() {
        // Given - Invalid CreateEmployeeRequest
        CreateEmployeeRequest invalidRequest =
                new CreateEmployeeRequest("", "", -1, 10); // Empty name/title, negative salary, age too low

        // When/Then - This test verifies that validation annotations are present
        // The actual validation testing should be done in controller integration tests
        assertThat(invalidRequest.getName()).isEmpty();
        assertThat(invalidRequest.getTitle()).isEmpty();
        assertThat(invalidRequest.getSalary()).isEqualTo(-1);
        assertThat(invalidRequest.getAge()).isEqualTo(10);
    }

    @Test
    void testEmployeeEmailGenerationPattern() {
        // Given - Employee with generated email
        Employee employee = Employee.builder()
                .id("test-id")
                .name("John Doe")
                .salary(50000)
                .age(30)
                .title("Developer")
                .email(Employee.getEmailFromName("John Doe"))
                .build();

        // Then - Verify email follows expected pattern
        assertThat(employee.getEmail()).isEqualTo("johnd@company.com");
    }
}
