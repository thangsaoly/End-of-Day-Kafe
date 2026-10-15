package com.kafe.dto;

import com.kafe.model.User;

// The signed-in user's profile. The app keeps using its Firebase ID token for requests.
public class AuthResponse {
    private final Long id;
    private final String name;
    private final String phoneNumber;
    private final String email;
    private final boolean emailConfirmed;
    private final String role;
    private final Long cafeId;

    public AuthResponse(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.phoneNumber = user.getPhoneNumber();
        this.email = user.getEmail();
        this.emailConfirmed = user.isEmailConfirmed();
        this.role = user.getRole() != null ? user.getRole() : "CUSTOMER";
        this.cafeId = user.getCafeId();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public boolean isEmailConfirmed() {
        return emailConfirmed;
    }

    public String getRole() {
        return role;
    }

    public Long getCafeId() {
        return cafeId;
    }
}
