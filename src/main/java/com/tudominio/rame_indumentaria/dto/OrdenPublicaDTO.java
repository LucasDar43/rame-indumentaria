package com.tudominio.rame_indumentaria.dto;

import com.tudominio.rame_indumentaria.model.EstadoOrden;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdenPublicaDTO {
    private Long id;
    private EstadoOrden estado;
    private String mpPreferenceId;
}
