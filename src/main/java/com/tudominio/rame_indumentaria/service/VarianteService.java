package com.tudominio.rame_indumentaria.service;

import com.tudominio.rame_indumentaria.dto.VarianteDTO;
import com.tudominio.rame_indumentaria.dto.VarianteRequestDTO;
import com.tudominio.rame_indumentaria.dto.mapper.VarianteMapper;
import com.tudominio.rame_indumentaria.model.Producto;
import com.tudominio.rame_indumentaria.model.Variante;
import com.tudominio.rame_indumentaria.repository.ProductoRepository;
import com.tudominio.rame_indumentaria.repository.VarianteRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VarianteService {

    private final VarianteRepository varianteRepository;
    private final ProductoRepository productoRepository;
    private final VarianteMapper varianteMapper;

    public List<VarianteDTO> listarPorProducto(Long productoId) {
        productoRepository.findById(productoId)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con id: " + productoId));
        return varianteMapper.toDTOList(
                varianteRepository.findByProductoIdAndActivoTrue(productoId)
        );
    }

    public VarianteDTO guardar(Long productoId, VarianteRequestDTO dto) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con id: " + productoId));
        Variante variante = varianteMapper.toEntity(dto);
        variante.setProducto(producto);
        variante.setActivo(true);
        return varianteMapper.toDTO(varianteRepository.save(variante));
    }

    public VarianteDTO actualizar(Long id, VarianteRequestDTO dto) {
        Variante variante = varianteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Variante no encontrada con id: " + id));
        variante.setTalle(dto.getTalle());
        variante.setColor(dto.getColor());
        variante.setStock(dto.getStock());
        variante.setSku(dto.getSku());
        return varianteMapper.toDTO(varianteRepository.save(variante));
    }

    public void eliminar(Long id) {
        Variante variante = varianteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Variante no encontrada con id: " + id));
        variante.setActivo(false);
        varianteRepository.save(variante);
    }
}
