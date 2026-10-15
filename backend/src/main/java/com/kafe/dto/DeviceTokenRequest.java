package com.kafe.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class DeviceTokenRequest {

    @NotBlank(message = "Token is required")
    @Size(max = 512, message = "Token is too long")
    private String token;

    public DeviceTokenRequest() {}

    public DeviceTokenRequest(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
