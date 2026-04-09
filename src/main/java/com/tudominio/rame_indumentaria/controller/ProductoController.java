package com.tudominio.rame_indumentaria.controller;

import com.tudominio.rame_indumentaria.dto.ImportacionResultadoDTO;
import com.tudominio.rame_indumentaria.dto.ProductoDTO;
import com.tudominio.rame_indumentaria.dto.ProductoRequestDTO;
import com.tudominio.rame_indumentaria.service.CloudinaryService;
import com.tudominio.rame_indumentaria.service.ImportacionService;
import com.tudominio.rame_indumentaria.service.ImportacionSimpleService;
import com.tudominio.rame_indumentaria.service.ProductoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final ImportacionService importacionService;
    private final ImportacionSimpleService importacionSimpleService;

    // GET paginado
    @GetMapping
    public ResponseEntity<Page<ProductoDTO>> listar(Pageable pageable) {
        return ResponseEntity.ok(productoService.listarPaginado(pageable));
    }

    @PostMapping(value = "/importar-simple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportacionResultadoDTO> importarSimple(
            @RequestPart("archivo") MultipartFile archivo
    ) throws IOException {
        return ResponseEntity.ok(importacionSimpleService.importarSimple(archivo));
    }

    // GET por ID
    @GetMapping("/{id}")
    public ResponseEntity<ProductoDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(productoService.buscarPorId(id));
    }

    // GET buscar por texto
    @GetMapping("/buscar")
    public ResponseEntity<List<ProductoDTO>> buscar(@RequestParam String q) {
        return ResponseEntity.ok(productoService.buscar(q));
    }

    // GET por categoria
    @GetMapping("/categoria/{categoria}")
    public ResponseEntity<List<ProductoDTO>> porCategoria(@PathVariable String categoria) {
        return ResponseEntity.ok(productoService.listarPorCategoria(categoria));
    }

    // POST crear con imagen
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductoDTO> crear(
            @RequestPart("producto") ProductoRequestDTO request,
            @RequestPart(value = "imagen", required = false) MultipartFile imagen
    ) throws IOException {
        String imagenUrl = null;
        if (imagen != null && !imagen.isEmpty()) {
            imagenUrl = cloudinaryService.subirImagen(imagen);
        }
        return ResponseEntity.ok(productoService.crear(request, imagenUrl));
    }

    // PUT actualizar
    @PutMapping("/{id}")
    public ResponseEntity<ProductoDTO> actualizar(
            @PathVariable Long id,
            @RequestBody ProductoRequestDTO dto) {
        return ResponseEntity.ok(productoService.actualizar(id, dto));
    }

    // DELETE soft delete
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        productoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // POST importar desde Excel
    @PostMapping(value = "/importar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportacionResultadoDTO> importar(
            @RequestPart("archivo") MultipartFile archivo
    ) throws IOException {
        return ResponseEntity.ok(importacionService.importarProductos(archivo));
    }


}
