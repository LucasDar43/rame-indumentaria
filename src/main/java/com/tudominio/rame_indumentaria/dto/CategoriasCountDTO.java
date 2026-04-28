package com.tudominio.rame_indumentaria.dto;

import lombok.*;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoriasCountDTO {
    private Map<String, Long> conteos;
}
