package com.tudominio.rame_indumentaria.dto;

import com.tudominio.rame_indumentaria.model.EstadoOrden;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdenResponseDTO {
    private Long id;
    private String nombreComprador;
    private String emailComprador;
    private Double total;
    private EstadoOrden estado;
    private String mpPreferenceId;
    private String initPoint;       // URL a la que redirigís al cliente
    private List<OrdenItemDTO> items;
    private LocalDateTime fechaCreacion;
}