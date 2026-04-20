package com.busfrontend.team;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One row in the Screens section of a team member's profile -
 * highlights the UI pages the member is responsible for.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ScreenEntry {
    private String name;     // "Customers List", "Add Customer", etc.
    private String url;      // Thymeleaf page URL
    private String icon;     // Bootstrap icon class
}
