package com.reliaquest.api.model;

import com.reliaquest.api.utils.RegexUtil;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DeleteEmployeeRequest {
    @NotBlank(message = "Employee name cannot be blank")
    @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
    private String name;

    @NotBlank(message = "Employee ID cannot be blank")
    @Pattern(regexp = RegexUtil.ALPHANUMERIC_HYPHEN_REGEX, message = "Invalid employee ID format")
    private String id;

    public DeleteEmployeeRequest(String id) {
        this.id = id;
    }

    public DeleteEmployeeRequest() {
        // Default constructor for deserialization
    }
}
