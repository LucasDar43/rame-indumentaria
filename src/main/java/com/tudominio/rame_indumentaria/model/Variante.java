package com.tudominio.rame_indumentaria.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "variantes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Variante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @NotBlank
    private String talle;

    @NotNull
    private Integer stock;

    private String sku;

    private Boolean activo = true;
    private String color;
}
