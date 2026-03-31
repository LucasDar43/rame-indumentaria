package com.tudominio.rame_indumentaria.service;

import com.tudominio.rame_indumentaria.dto.LoginResponseDTO;
import com.tudominio.rame_indumentaria.model.Usuario;
import com.tudominio.rame_indumentaria.repository.UsuarioRepository;
import com.tudominio.rame_indumentaria.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginResponseDTO login(String email, String password) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales incorrectas"));

        if (!passwordEncoder.matches(password, usuario.getPassword())) {
            throw new BadCredentialsException("Credenciales incorrectas");
        }

        String token = jwtService.generateToken(usuario);

        return LoginResponseDTO.builder()
                .token(token)
                .email(usuario.getEmail())
                .rol(usuario.getRol().name())
                .build();
    }
}