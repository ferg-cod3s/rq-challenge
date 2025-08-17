package com.reliaquest.api.utils;

import java.util.ArrayList;
import java.util.List;
import lombok.Setter;

@Setter
public class ApiResponse<T> {
    private T data;
    private String status;

    public T getData() {
        return data;
    }

    public String getStatus() {
        return status;
    }

    // Overloaded method for list responses - returns empty list if data is null
    @SuppressWarnings("unchecked")
    public List<T> getDataAsList() {
        if (data instanceof List) {
            return (List<T>) data;
        }
        return new ArrayList<>();
    }
}
