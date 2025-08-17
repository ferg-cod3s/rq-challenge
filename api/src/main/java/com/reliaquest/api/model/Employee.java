package com.reliaquest.api.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(Employee.PrefixNamingStrategy.class)
public class Employee {

    private String id;
    private String name;
    private Integer salary;
    private Integer age;
    private String title;
    private String email;

    public static class PrefixNamingStrategy extends PropertyNamingStrategies.NamingBase {
        @Override
        public String translate(String propertyName) {
            if ("id".equals(propertyName)) {
                return propertyName;
            }
            return "employee_" + propertyName;
        }
    }

    public static String getEmailFromName(String name) {
        // use first name and first initial of last name
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }

        String[] nameParts = name.trim().split("\\s+");
        if (nameParts.length == 0 || nameParts[0].isEmpty()) {
            throw new IllegalArgumentException("Name must contain at least one character");
        }

        String firstName = nameParts[0].toLowerCase();
        String lastNameInitial = "";

        if (nameParts.length > 1) {
            String lastName = nameParts[nameParts.length - 1].trim();
            if (!lastName.isEmpty()) {
                lastNameInitial = lastName.substring(0, 1).toLowerCase();
            }
        }

        return firstName + lastNameInitial + "@company.com";
    }
}
