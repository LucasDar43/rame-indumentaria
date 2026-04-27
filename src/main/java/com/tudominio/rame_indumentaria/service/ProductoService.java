package com.tudominio.rame_indumentaria.service;

import com.tudominio.rame_indumentaria.dto.FiltrosDisponiblesDTO;
import com.tudominio.rame_indumentaria.dto.ProductoDTO;
import com.tudominio.rame_indumentaria.dto.ProductoFiltrosDTO;
import com.tudominio.rame_indumentaria.dto.ProductoRequestDTO;
import com.tudominio.rame_indumentaria.dto.mapper.ProductoMapper;
import com.tudominio.rame_indumentaria.model.Variante;
import com.tudominio.rame_indumentaria.model.Producto;
import com.tudominio.rame_indumentaria.repository.ProductoRepository;
import com.tudominio.rame_indumentaria.repository.VarianteRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    public Page<ProductoDTO> listarConFiltros(ProductoFiltrosDTO filtros, Pageable pageable) {
        Specification<Producto> spec = buildSpecification(filtros);
        Pageable pageableConOrden = aplicarOrden(filtros.getOrdenar(), pageable);
        return productoRepository.findAll(spec, pageableConOrden)
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

    private Specification<Producto> buildSpecification(ProductoFiltrosDTO filtros) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new java.util.ArrayList<>();

            predicates.add(cb.isTrue(root.get("activo")));

            if (filtros.getQ() != null && !filtros.getQ().isBlank()) {
                String pattern = "%" + filtros.getQ().toLowerCase().trim() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("nombre")), pattern),
                        cb.like(cb.lower(root.get("marca")), pattern)
                ));
            }

            if (filtros.getCategoria() != null && !filtros.getCategoria().isBlank()) {
                predicates.add(cb.equal(
                        cb.lower(root.get("categoria")),
                        filtros.getCategoria().toLowerCase().trim()
                ));
            }

            if (filtros.getMarca() != null && !filtros.getMarca().isBlank()) {
                predicates.add(cb.equal(
                        cb.lower(root.get("marca")),
                        filtros.getMarca().toLowerCase().trim()
                ));
            }

            boolean filtraColor = filtros.getColor() != null && !filtros.getColor().isBlank();
            boolean filtraTalle = filtros.getTalle() != null && !filtros.getTalle().isBlank();
            if (filtraColor || filtraTalle) {
                Join<Producto, Variante> variantesJoin = root.join("variantes", JoinType.INNER);
                predicates.add(cb.isTrue(variantesJoin.get("activo")));

                if (filtraColor) {
                    predicates.add(cb.equal(
                            cb.lower(variantesJoin.get("color")),
                            filtros.getColor().toLowerCase().trim()
                    ));
                }

                if (filtraTalle) {
                    predicates.add(cb.equal(
                            cb.lower(variantesJoin.get("talle")),
                            filtros.getTalle().toLowerCase().trim()
                    ));
                }

                query.distinct(true);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Pageable aplicarOrden(String ordenar, Pageable pageable) {
        if (ordenar == null || ordenar.isBlank()) {
            return pageable;
        }

        Sort sort = switch (ordenar.trim()) {
            case "precio-asc" -> Sort.by(Sort.Direction.ASC, "precio");
            case "precio-desc" -> Sort.by(Sort.Direction.DESC, "precio");
            case "nombre-asc" -> Sort.by(Sort.Direction.ASC, "nombre");
            case "nombre-desc" -> Sort.by(Sort.Direction.DESC, "nombre");
            default -> Sort.unsorted();
        };

        return org.springframework.data.domain.PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sort
        );
    }

    @Transactional(readOnly = true)
    public FiltrosDisponiblesDTO getFiltrosDisponibles() {
        List<Producto> todosActivos = productoRepository.findByActivoTrue();

        List<String> marcas = todosActivos.stream()
                .map(Producto::getMarca)
                .filter(m -> m != null && !m.isBlank())
                .distinct()
                .sorted()
                .collect(java.util.stream.Collectors.toList());

        List<String> colores = todosActivos.stream()
                .flatMap(p -> p.getVariantes().stream())
                .filter(v -> v.getActivo() && v.getColor() != null && !v.getColor().isBlank())
                .map(Variante::getColor)
                .distinct()
                .sorted()
                .collect(java.util.stream.Collectors.toList());

        List<String> talles = todosActivos.stream()
                .flatMap(p -> p.getVariantes().stream())
                .filter(v -> v.getActivo() && v.getTalle() != null && !v.getTalle().isBlank())
                .map(Variante::getTalle)
                .distinct()
                .sorted()
                .collect(java.util.stream.Collectors.toList());

        return FiltrosDisponiblesDTO.builder()
                .marcas(marcas)
                .colores(colores)
                .talles(talles)
                .build();
    }
}
