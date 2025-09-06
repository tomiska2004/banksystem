package org.example.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.example.model.User;
import org.example.security.JwtUtil;
import org.example.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    public UserController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody User user) {  // ← Add @Valid
        try {
            User savedUser = userService.register(user);
            return ResponseEntity.ok(savedUser);
        } catch (Exception e) {
            // Let validation errors be handled globally; this catch is for business logic exceptions
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User user) {
        // Login doesn't need @Valid since only username/password are used, but we validate manually
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Username is required");
        }
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Password is required");
        }

        boolean authenticated = userService.authenticate(user.getUsername(), user.getPassword());
        if (authenticated) {
            Long userId = userService.getId(user.getUsername());
            if (userId == null) {
                return ResponseEntity.status(500).body("User ID not found");
            }
            String token = jwtUtil.generateToken(user.getUsername(), userId);
            return ResponseEntity.ok(token);
        } else {
            return ResponseEntity.status(401).body("Invalid credentials");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUser(@PathVariable @Min(1) Long id) {  // ← Validate path variable
        return userService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}