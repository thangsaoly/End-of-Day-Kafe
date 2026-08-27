package com.kafe.dto;

public class AuthResponse {
    private String token;
    private Long id;
    private String name;
    private String phoneNumber;
    private String email;
    private boolean emailConfirmed;
    private String role;

    public AuthResponse(String token, Long id, String name, String phoneNumber, String email, boolean emailConfirmed) {
        this(token, id, name, phoneNumber, email, emailConfirmed, "CUSTOMER");
    }

    public AuthResponse(String token, Long id, String name, String phoneNumber, String email, boolean emailConfirmed, String role) {
        this.token = token;
        this.id = id;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.emailConfirmed = emailConfirmed;
        this.role = role != null ? role : "CUSTOMER";
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isEmailConfirmed() {
        return emailConfirmed;
    }

    public void setEmailConfirmed(boolean emailConfirmed) {
        this.emailConfirmed = emailConfirmed;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
