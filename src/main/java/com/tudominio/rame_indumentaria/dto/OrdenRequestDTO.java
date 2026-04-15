package com.tudominio.rame_indumentaria.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdenRequestDTO {

    @NotBlank
    private String nombreComprador;

    @NotBlank
    @Email
    private String emailComprador;

    private String telefonoComprador;
    private String direccionEnvio;
    private String ciudadEnvio;
    private String provinciaEnvio;
    private BigDecimal costoEnvio;
    private BigDecimal totalFinal;

    @NotEmpty
    private List<OrdenItemRequestDTO> items;
}
