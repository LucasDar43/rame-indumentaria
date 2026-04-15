package com.tudominio.rame_indumentaria.controller;

import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.tudominio.rame_indumentaria.dto.OrdenRequestDTO;
import com.tudominio.rame_indumentaria.dto.OrdenResponseDTO;
import com.tudominio.rame_indumentaria.service.OrdenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ordenes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OrdenController {

    private final OrdenService ordenService;

    @PostMapping
    public ResponseEntity<OrdenResponseDTO> crear(@RequestBody @Valid OrdenRequestDTO dto)
            throws MPException, MPApiException {
        return ResponseEntity.status(HttpStatus.CREATED).body(ordenService.crearOrden(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrdenResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(ordenService.buscarPorId(id));
    }

    @GetMapping
    public ResponseEntity<Page<OrdenResponseDTO>> listar(
            @PageableDefault(size = 10, sort = "fechaCreacion", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(ordenService.listarPaginado(pageable));
    }

}
