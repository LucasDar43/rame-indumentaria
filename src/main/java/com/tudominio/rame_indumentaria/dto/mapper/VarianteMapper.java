package com.tudominio.rame_indumentaria.dto.mapper;

import com.tudominio.rame_indumentaria.dto.VarianteDTO;
import com.tudominio.rame_indumentaria.dto.VarianteRequestDTO;
import com.tudominio.rame_indumentaria.model.Variante;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class VarianteMapper {

    public VarianteDTO toDTO(Variante variante) {
        return VarianteDTO.builder()
                .id(variante.getId())
                .productoId(variante.getProducto().getId())
                .talle(variante.getTalle())
                .color(variante.getColor())
                .stock(variante.getStock())
                .sku(variante.getSku())
                .activo(variante.getActivo())
                .build();
    }

    public Variante toEntity(VarianteRequestDTO dto) {
        return Variante.builder()
                .talle(dto.getTalle())
                .color(dto.getColor())
                .stock(dto.getStock())
                .sku(dto.getSku())
                .build();
    }

    public List<VarianteDTO> toDTOList(List<Variante> variantes) {
        return variantes.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
}
