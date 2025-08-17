package com.reliaquest.api.model;

import com.reliaquest.api.utils.RegexUtil;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateEmployeeRequest {
    @NotBlank(message = "Name cannot be blank")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    @Pattern(regexp = RegexUtil.LETTERS_AND_SPACES_REGEX, message = "Name can only contain letters and spaces")
    private String name;

    @NotBlank(message = "Title cannot be blank")
    @Size(min = 2, max = 100, message = "Title must be between 2 and 100 characters")
    private String title;

    @NotNull(message = "Salary cannot be null") @Min(value = 1, message = "Salary must be at least $1")
    @Max(value = 10000000, message = "Salary cannot exceed $10,000,000")
    private Integer salary;

    @NotNull(message = "Age cannot be null") @Min(value = 16, message = "Employee must be at least 16 years old")
    @Max(value = 75, message = "Employee cannot be older than 75")
    private Integer age;

    public CreateEmployeeRequest() {
        // Default constructor for deserialization
    }

    public CreateEmployeeRequest(String name, String title, Integer salary, Integer age) {
        this.name = name;
        this.title = title;
        this.salary = salary;
        this.age = age;
    }

    public String nameToEmail() {
        return Employee.getEmailFromName(name);
    }
}
