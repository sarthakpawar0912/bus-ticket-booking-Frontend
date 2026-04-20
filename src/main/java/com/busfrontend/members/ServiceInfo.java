package com.busfrontend.members;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceInfo {
    private String key;           // "customers" (URL-safe)
    private String name;          // "Customers"
    private String icon;          // bootstrap icon class
    private String description;
    private List<Operation> operations;
}
