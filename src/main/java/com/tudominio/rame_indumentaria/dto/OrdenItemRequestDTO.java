package com.tudominio.rame_indumentaria.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdenItemRequestDTO {

    @NotNull
    private Long productoId;

    @NotNull
    @Min(1)
    private Integer cantidad;
}