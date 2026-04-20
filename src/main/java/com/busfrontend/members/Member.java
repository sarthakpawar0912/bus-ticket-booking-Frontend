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
public class Member {
    private Integer id;
    private String name;
    private String role;
    private String image;        // URL to profile image
    private String initials;     // fallback if image fails
    private String color;        // tailwind-like color for avatar
    private String summary;      // short text of services owned
    private List<ServiceInfo> services;
}
