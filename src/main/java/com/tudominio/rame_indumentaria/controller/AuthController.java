package com.tudominio.rame_indumentaria.controller;

import com.tudominio.rame_indumentaria.dto.LoginRequestDTO;
import com.tudominio.rame_indumentaria.dto.LoginResponseDTO;
import com.tudominio.rame_indumentaria.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid LoginRequestDTO dto) {
        String token = authService.login(dto.getEmail(), dto.getPassword());
        return ResponseEntity.ok(LoginResponseDTO.builder()
                .token(token)
                .email(dto.getEmail())
                .rol("ADMIN")
                .build());
    }

}