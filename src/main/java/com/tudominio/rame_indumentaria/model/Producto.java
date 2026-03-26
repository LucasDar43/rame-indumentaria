package com.tudominio.rame_indumentaria.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "productos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @NotNull
    private Double precio;

    private String marca;

    private String categoria;

    private String subcategoria;

    @Column(name = "imagen_url")
    private String imagenUrl;

    @ElementCollection
    @CollectionTable(name = "producto_imagenes")
    @Builder.Default
    private List<String> imagenes = new ArrayList<>();

    @Builder.Default
    private Boolean activo = true;

    private LocalDateTime fechaCreacion;

    @PrePersist
    public void prePersist() {
        fechaCreacion = LocalDateTime.now();
    }
}
