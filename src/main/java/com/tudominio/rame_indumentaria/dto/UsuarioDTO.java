package com.tudominio.rame_indumentaria.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioDTO {
    private String email;
    private String rol;
}
