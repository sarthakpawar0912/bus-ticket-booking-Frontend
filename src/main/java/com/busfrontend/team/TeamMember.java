package com.busfrontend.team;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TeamMember {
    private String slug;           // URL slug, e.g. "sarthak-pawar"
    private String name;           // "Sarthak Pawar"
    private String role;           // "Customer Module Developer"
    private String photo;          // Avatar URL
    private String initials;       // "SP" fallback
    private String color;          // Card accent color
    private List<String> modules;  // ["Customers", "Addresses"]
    private List<String> responsibilities;
    private List<EndpointEntry> endpoints;
    private List<ScreenEntry> screens;
}
