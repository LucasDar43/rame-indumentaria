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
import com.tudominio.rame_indumentaria.model.ImagenProducto;
import com.tudominio.rame_indumentaria.repository.ImagenProductoRepository;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final VarianteRepository varianteRepository;
    private final ProductoMapper productoMapper;
    private final ImagenProductoRepository imagenProductoRepository;

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

    public ProductoDTO agregarImagenGaleria(Long id, String url) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con id: " + id));

        int siguienteOrden = producto.getImagenes().size() + 1;

        ImagenProducto imagen = ImagenProducto.builder()
                .producto(producto)
                .url(url)
                .orden(siguienteOrden)
                .build();

        producto.getImagenes().add(imagen);
        return productoMapper.toDTO(productoRepository.save(producto));
    }

    public ProductoDTO eliminarImagenGaleria(Long id, Long imagenId) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con id: " + id));

        producto.getImagenes().removeIf(img -> img.getId().equals(imagenId));

        // Reordenar
        for (int i = 0; i < producto.getImagenes().size(); i++) {
            producto.getImagenes().get(i).setOrden(i + 1);
        }

        return productoMapper.toDTO(productoRepository.save(producto));
    }

    public void eliminar(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con id: " + id));
        producto.setActivo(false);
        productoRepository.save(producto);
    }
}
