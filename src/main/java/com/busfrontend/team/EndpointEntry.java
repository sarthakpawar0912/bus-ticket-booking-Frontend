package com.busfrontend.team;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One row in the Functional Endpoints section of a team member's profile.
 * Every entry points to a REAL Thymeleaf UI page (never raw JSON).
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class EndpointEntry {
    private String method;        // GET, GET ALL, POST, PUT, PATCH, SEARCH, DOWNLOAD
    private String apiPath;       // Backend REST path (shown as reference)
    private String description;
    private String uiUrl;         // Existing Thymeleaf page the "Perform" button opens
    private String actionLabel;   // Button label (e.g., "Open List", "Add New", "Search")
}
