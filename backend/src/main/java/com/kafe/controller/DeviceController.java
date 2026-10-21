package com.kafe.controller;

import com.kafe.dto.DeviceTokenRequest;
import com.kafe.model.User;
import com.kafe.service.DeviceTokenService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceTokenService deviceTokenService;

    public DeviceController(DeviceTokenService deviceTokenService) {
        this.deviceTokenService = deviceTokenService;
    }

    @PostMapping
    public ResponseEntity<Void> register(@Valid @RequestBody DeviceTokenRequest request) {
        deviceTokenService.register(request.getToken(), currentUser());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/unregister")
    public ResponseEntity<Void> unregister(@Valid @RequestBody DeviceTokenRequest request) {
        deviceTokenService.unregister(request.getToken(), currentUser());
        return ResponseEntity.noContent().build();
    }

    private User currentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
