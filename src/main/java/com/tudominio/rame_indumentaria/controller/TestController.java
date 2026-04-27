package com.tudominio.rame_indumentaria.controller;

import com.tudominio.rame_indumentaria.model.EstadoOrden;
import com.tudominio.rame_indumentaria.model.Orden;
import com.tudominio.rame_indumentaria.repository.OrdenRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    private final OrdenRepository ordenRepository;

    @Value("${app.modo-test:false}")
    private boolean modoTest;

    @PostMapping("/aprobar-orden/{id}")
    public ResponseEntity<Void> aprobar(@PathVariable Long id) {
        if (!modoTest) {
            return ResponseEntity.status(403).build();
        }
        Orden orden = ordenRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Orden no encontrada: " + id));
        orden.setEstado(EstadoOrden.APROBADO);
        ordenRepository.save(orden);
        return ResponseEntity.ok().build();
    }
}
