package leave_management_project.leave_management.controller;


import leave_management_project.leave_management.config.JwtUtils;
import leave_management_project.leave_management.dto.JwtResponseDto;
import leave_management_project.leave_management.dto.LoginRequestDto;
import leave_management_project.leave_management.dto.RegisterRequestDto;
import leave_management_project.leave_management.model.User;
import leave_management_project.leave_management.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtUtils jwtUtils;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequestDto request) {
        if (userService.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body("Email already registered");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setRole("STAFF"); // default role
        User savedUser = userService.registerUser(user);
        return ResponseEntity.ok(savedUser);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDto request) {
        Optional<User> userOpt = userService.findByEmail(request.getEmail());

        if (userOpt.isEmpty() || !userService.checkPassword(userOpt.get(), request.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }

        String token = jwtUtils.generateToken(userOpt.get());
        return ResponseEntity.ok(new JwtResponseDto(token));
    }
}
