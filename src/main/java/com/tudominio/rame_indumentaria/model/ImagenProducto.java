package com.tudominio.rame_indumentaria.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "imagen_producto")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImagenProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private Integer orden;
}