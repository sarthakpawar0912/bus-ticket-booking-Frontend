package com.busfrontend.members;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldDef {
    private String name;
    private String label;
    private String type;        // text, email, tel, number, textarea, datetime-local
    private String placeholder;
    private boolean required;
    private String helpText;
    private String valueType;   // "string", "integer", "number"  -> JSON type
}
