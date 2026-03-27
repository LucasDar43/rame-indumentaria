package com.tudominio.rame_indumentaria.controller;

import com.tudominio.rame_indumentaria.dto.ProductoDTO;
import com.tudominio.rame_indumentaria.dto.ProductoRequestDTO;
import com.tudominio.rame_indumentaria.service.CloudinaryService;
import com.tudominio.rame_indumentaria.service.ProductoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProductoController {

    private final ProductoService productoService;
    private final CloudinaryService cloudinaryService;

    // GET /api/productos?page=0&size=20
    @GetMapping
    public ResponseEntity<Page<ProductoDTO>> listar(Pageable pageable) {
        return ResponseEntity.ok(productoService.listarPaginado(pageable));
    }

    // GET /api/productos/{id}
    @GetMapping("/{id}")
    public ResponseEntity<ProductoDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(productoService.buscarPorId(id));
    }

    // GET /api/productos/buscar?q=nike
    @GetMapping("/buscar")
    public ResponseEntity<List<ProductoDTO>> buscar(@RequestParam String q) {
        return ResponseEntity.ok(productoService.buscar(q));
    }

    // GET /api/productos/categoria/{categoria}
    @GetMapping("/categoria/{categoria}")
    public ResponseEntity<List<ProductoDTO>> porCategoria(@PathVariable String categoria) {
        return ResponseEntity.ok(productoService.listarPorCategoria(categoria));
    }

    // POST /api/productos
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductoDTO> crear(
            @RequestPart("producto") @Valid ProductoRequestDTO request,
            @RequestPart(value = "imagen", required = false) MultipartFile imagen
    ) throws IOException {
        String imagenUrl = null;
        if (imagen != null && !imagen.isEmpty()) {
            imagenUrl = cloudinaryService.subirImagen(imagen);
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productoService.crear(request, imagenUrl));
    }

    // PUT /api/productos/{id}
    @PutMapping("/{id}")
    public ResponseEntity<ProductoDTO> actualizar(
            @PathVariable Long id,
            @RequestBody @Valid ProductoRequestDTO dto
    ) {
        return ResponseEntity.ok(productoService.actualizar(id, dto));
    }

    // DELETE /api/productos/{id}  (soft delete)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        productoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // POST /api/productos/{id}/imagenes  (galería)
    @PostMapping(value = "/{id}/imagenes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductoDTO> agregarImagen(
            @PathVariable Long id,
            @RequestPart("imagen") MultipartFile imagen
    ) throws IOException {
        String url = cloudinaryService.subirImagen(imagen);
        return ResponseEntity.ok(productoService.agregarImagenGaleria(id, url));
    }

    // DELETE /api/productos/{id}/imagenes/{imagenId}
    @DeleteMapping("/{id}/imagenes/{imagenId}")
    public ResponseEntity<ProductoDTO> eliminarImagen(
            @PathVariable Long id,
            @PathVariable Long imagenId
    ) {
        return ResponseEntity.ok(productoService.eliminarImagenGaleria(id, imagenId));
    }
}
