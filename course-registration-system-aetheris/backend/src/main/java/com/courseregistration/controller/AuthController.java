package com.courseregistration.controller;

import com.courseregistration.dto.AuthResponse;
import com.courseregistration.dto.LoginRequest;
import com.courseregistration.dto.RegisterRequest;
import com.courseregistration.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** Public self-registration — always creates a STUDENT account. */
    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        return authService.register(req);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }
}
