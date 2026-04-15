package com.tudominio.rame_indumentaria.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "ordenes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Orden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Datos del comprador (no requiere auth por ahora)
    private String nombreComprador;
    private String emailComprador;
    private String telefonoComprador;

    // Datos de envío
    private String direccionEnvio;
    private String ciudadEnvio;
    private String provinciaEnvio;

    private BigDecimal costoEnvio;
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    private EstadoOrden estado;

    // IDs de MercadoPago
    private String mpPreferenceId;
    private String mpPaymentId;

    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrdenItem> items;

    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    @PrePersist
    public void prePersist() {
        fechaCreacion = LocalDateTime.now();
        fechaActualizacion = LocalDateTime.now();
        estado = EstadoOrden.PENDIENTE;
    }

    @PreUpdate
    public void preUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }
}
