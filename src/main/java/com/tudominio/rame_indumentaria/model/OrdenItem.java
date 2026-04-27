package com.tudominio.rame_indumentaria.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "orden_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdenItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "orden_id", nullable = false)
    private Orden orden;

    private Long productoId;
    private Long varianteId;
    private String nombreProducto;
    private String talle;
    private String color;
    private String imagenUrl;
    private Integer cantidad;
    private BigDecimal precioUnitario;
}
