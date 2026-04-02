package com.tudominio.rame_indumentaria.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FilaErrorDTO {
    private int fila;
    private String mensaje;
}
