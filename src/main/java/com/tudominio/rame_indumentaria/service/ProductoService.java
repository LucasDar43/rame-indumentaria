package com.tudominio.rame_indumentaria.service;

import com.tudominio.rame_indumentaria.dto.ProductoDTO;
import com.tudominio.rame_indumentaria.dto.ProductoRequestDTO;
import com.tudominio.rame_indumentaria.dto.mapper.ProductoMapper;
import com.tudominio.rame_indumentaria.model.Producto;
import com.tudominio.rame_indumentaria.repository.ProductoRepository;
import com.tudominio.rame_indumentaria.repository.VarianteRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final VarianteRepository varianteRepository;
    private final ProductoMapper productoMapper;

    public Page<ProductoDTO> listarPaginado(Pageable pageable) {
        return productoRepository.findByActivoTrue(pageable)
                .map(productoMapper::toDTO);
    }

    public ProductoDTO buscarPorId(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con id: " + id));
        return productoMapper.toDTO(producto);
    }

    public List<ProductoDTO> buscar(String texto) {
        return productoMapper.toDTOList(productoRepository.buscar(texto));
    }

    public List<ProductoDTO> listarPorCategoria(String categoria) {
        return productoMapper.toDTOList(
                productoRepository.findByCategoriaAndActivoTrue(categoria)
        );
    }

    public ProductoDTO guardar(ProductoRequestDTO dto) {
        Producto producto = productoMapper.toEntity(dto);
        producto.setActivo(true);
        return productoMapper.toDTO(productoRepository.save(producto));
    }

    public ProductoDTO actualizar(Long id, ProductoRequestDTO dto) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con id: " + id));
        producto.setNombre(dto.getNombre());
        producto.setDescripcion(dto.getDescripcion());
        producto.setPrecio(dto.getPrecio());
        producto.setMarca(dto.getMarca());
        producto.setCategoria(dto.getCategoria());
        producto.setSubcategoria(dto.getSubcategoria());
        return productoMapper.toDTO(productoRepository.save(producto));
    }

    public ProductoDTO crear(ProductoRequestDTO request, String imagenUrl) {
        Producto producto = productoMapper.toEntity(request);
        producto.setImagenUrl(imagenUrl);
        producto.setActivo(true);
        Producto guardado = productoRepository.save(producto);
        return productoMapper.toDTO(guardado);
    }

    public void eliminar(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con id: " + id));
        producto.setActivo(false);
        productoRepository.save(producto);
    }
}
