package com.tudominio.rame_indumentaria.controller;

import com.tudominio.rame_indumentaria.dto.LoginRequestDTO;
import com.tudominio.rame_indumentaria.dto.LoginResponseDTO;
import com.tudominio.rame_indumentaria.dto.UsuarioDTO;
import com.tudominio.rame_indumentaria.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid LoginRequestDTO dto) {
        return ResponseEntity.ok(authService.login(dto.getEmail(), dto.getPassword()));
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioDTO> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        com.tudominio.rame_indumentaria.model.Usuario usuario =
                (com.tudominio.rame_indumentaria.model.Usuario) authentication.getPrincipal();

        return ResponseEntity.ok(
                UsuarioDTO.builder()
                        .email(usuario.getEmail())
                        .rol(usuario.getRol().name())
                        .build()
        );
    }
}
