package com.tudominio.rame_indumentaria.controller;

import com.tudominio.rame_indumentaria.dto.ProductoDTO;
import com.tudominio.rame_indumentaria.dto.ProductoRequestDTO;
import com.tudominio.rame_indumentaria.service.CloudinaryService;
import com.tudominio.rame_indumentaria.service.ProductoService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService productoService;
    private final CloudinaryService cloudinaryService;

    public ProductoController(ProductoService productoService, CloudinaryService cloudinaryService) {
        this.productoService = productoService;
        this.cloudinaryService = cloudinaryService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductoDTO> crear(
            @RequestPart("producto") ProductoRequestDTO request,
            @RequestPart(value = "imagen", required = false) MultipartFile imagen
    ) throws IOException {
        String imagenUrl = null;
        if (imagen != null && !imagen.isEmpty()) {
            imagenUrl = cloudinaryService.subirImagen(imagen);
        }
        ProductoDTO creado = productoService.crear(request, imagenUrl);
        return ResponseEntity.ok(creado);
    }
}
