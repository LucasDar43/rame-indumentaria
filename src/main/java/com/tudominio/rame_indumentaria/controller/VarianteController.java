package com.tudominio.rame_indumentaria.controller;

import com.tudominio.rame_indumentaria.dto.VarianteDTO;
import com.tudominio.rame_indumentaria.dto.VarianteRequestDTO;
import com.tudominio.rame_indumentaria.service.VarianteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/productos/{productoId}/variantes")
@RequiredArgsConstructor
public class VarianteController {

    private final VarianteService varianteService;

    @GetMapping
    public ResponseEntity<List<VarianteDTO>> listar(@PathVariable Long productoId) {
        return ResponseEntity.ok(varianteService.listarPorProducto(productoId));
    }

    @PostMapping
    public ResponseEntity<VarianteDTO> crear(
            @PathVariable Long productoId,
            @RequestBody @Valid VarianteRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(varianteService.guardar(productoId, dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VarianteDTO> actualizar(
            @PathVariable Long productoId,
            @PathVariable Long id,
            @RequestBody @Valid VarianteRequestDTO dto) {
        return ResponseEntity.ok(varianteService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long productoId,
            @PathVariable Long id) {
        varianteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
