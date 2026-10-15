package com.kafe.dto;

import jakarta.validation.constraints.Size;

// Optional details used only the first time a Firebase user starts a session
public class SessionRequest {

    @Size(max = 100, message = "Name is too long")
    private String name;

    // CUSTOMER (default) or MERCHANT. A new merchant can do nothing until linked to a cafe.
    private String role;

    public SessionRequest() {}

    public SessionRequest(String name, String role) {
        this.name = name;
        this.role = role;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
