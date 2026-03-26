package com.tudominio.rame_indumentaria.dto.mapper;

import com.tudominio.rame_indumentaria.dto.ProductoDTO;
import com.tudominio.rame_indumentaria.dto.ProductoRequestDTO;
import com.tudominio.rame_indumentaria.model.Producto;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ProductoMapper {

    public ProductoDTO toDTO(Producto producto) {
        return ProductoDTO.builder()
                .id(producto.getId())
                .nombre(producto.getNombre())
                .descripcion(producto.getDescripcion())
                .precio(producto.getPrecio())
                .marca(producto.getMarca())
                .categoria(producto.getCategoria())
                .subcategoria(producto.getSubcategoria())
                .imagenes(producto.getImagenes())
                .activo(producto.getActivo())
                .fechaCreacion(producto.getFechaCreacion())
                .build();
    }

    public Producto toEntity(ProductoRequestDTO dto) {
        return Producto.builder()
                .nombre(dto.getNombre())
                .descripcion(dto.getDescripcion())
                .precio(dto.getPrecio())
                .marca(dto.getMarca())
                .categoria(dto.getCategoria())
                .subcategoria(dto.getSubcategoria())
                .imagenes(dto.getImagenes() != null ? dto.getImagenes() : new ArrayList<>())
                .build();
    }

    public List<ProductoDTO> toDTOList(List<Producto> productos) {
        return productos.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

}
