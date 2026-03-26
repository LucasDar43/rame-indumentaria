package com.tudominio.rame_indumentaria.service;

import com.tudominio.rame_indumentaria.model.Rol;
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

    public String login(String email, String password) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales incorrectas"));

        if (!passwordEncoder.matches(password, usuario.getPassword())) {
            throw new BadCredentialsException("Credenciales incorrectas");
        }

        return jwtService.generateToken(usuario);
    }

    public void registrarAdmin(String email, String password) {
        if (usuarioRepository.findByEmail(email).isPresent()) {
            throw new IllegalStateException("Ya existe un usuario con ese email");
        }
        Usuario usuario = Usuario.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .rol(Rol.ADMIN)
                .build();
        usuarioRepository.save(usuario);
    }
}