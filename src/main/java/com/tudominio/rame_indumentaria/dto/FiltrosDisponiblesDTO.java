package com.tudominio.rame_indumentaria.dto;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FiltrosDisponiblesDTO {
    private List<String> marcas;
    private List<String> colores;
    private List<String> talles;
}
