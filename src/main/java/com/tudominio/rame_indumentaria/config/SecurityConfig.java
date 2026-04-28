package com.tudominio.rame_indumentaria.config;

import com.tudominio.rame_indumentaria.security.JwtAuthFilter;
import com.tudominio.rame_indumentaria.security.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;

    @Value("${frontend.url}")
    private String frontendUrl;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, UserDetailsServiceImpl userDetailsService) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Auth publica
                        .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
                        .requestMatchers("/api/auth/**").permitAll()

                        // Productos - lectura publica
                        .requestMatchers(HttpMethod.GET, "/api/productos/**").permitAll()

                        // Variantes - lectura publica
                        .requestMatchers(HttpMethod.GET,
                                "/api/productos/*/variantes").permitAll()

                        // Ordenes - creacion publica
                        .requestMatchers(HttpMethod.POST, "/api/ordenes").permitAll()

                        // Ordenes - estado publico (solo estado, sin datos personales)
                        .requestMatchers(HttpMethod.GET,
                                "/api/ordenes/*/estado").permitAll()

                        // Webhook - publico para MercadoPago
                        .requestMatchers("/api/webhook/**").permitAll()

                        // Test - solo en modo test
                        .requestMatchers("/api/test/**").permitAll()

                        // Productos - escritura solo ADMIN
                        .requestMatchers(HttpMethod.POST,
                                "/api/productos/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,
                                "/api/productos/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE,
                                "/api/productos/**").hasRole("ADMIN")

                        // Variantes - escritura solo ADMIN
                        .requestMatchers(HttpMethod.POST,
                                "/api/productos/*/variantes/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,
                                "/api/productos/*/variantes/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE,
                                "/api/productos/*/variantes/**").hasRole("ADMIN")

                        // Ordenes - listado y detalle completo solo ADMIN
                        .requestMatchers(HttpMethod.GET,
                                "/api/ordenes").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET,
                                "/api/ordenes/*").hasRole("ADMIN")

                        // Todo lo demas requiere autenticacion
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));

        return http.build();
    }

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        String origin = frontendUrl.endsWith("/")
                ? frontendUrl.substring(0, frontendUrl.length() - 1)
                : frontendUrl;

        config.setAllowedOrigins(java.util.List.of(origin));
        config.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(java.util.List.of("*"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }
}
