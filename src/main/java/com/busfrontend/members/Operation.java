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
public class Operation {
    private String name;          // "Create", "Get All", "Get By ID", "Update", "Delete"
    private String method;        // GET, POST, PUT, DELETE, PATCH
    private String endpoint;      // "/api/customers" or "/api/customers/{id}"
    private String inputKind;     // NONE | ID | BODY | ID_AND_BODY | PDF_DOWNLOAD
    private String description;
    private List<FieldDef> fields;
}
