package com.tudominio.rame_indumentaria.service;

import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.preference.Preference;
import com.tudominio.rame_indumentaria.dto.OrdenItemDTO;
import com.tudominio.rame_indumentaria.dto.OrdenItemRequestDTO;
import com.tudominio.rame_indumentaria.dto.OrdenRequestDTO;
import com.tudominio.rame_indumentaria.dto.OrdenResponseDTO;
import com.tudominio.rame_indumentaria.model.Orden;
import com.tudominio.rame_indumentaria.model.OrdenItem;
import com.tudominio.rame_indumentaria.model.Producto;
import com.tudominio.rame_indumentaria.repository.OrdenRepository;
import com.tudominio.rame_indumentaria.repository.ProductoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrdenService {

    private static final BigDecimal UMBRAL_ENVIO_GRATIS = BigDecimal.valueOf(50000);
    private static final BigDecimal COSTO_ENVIO_FIJO = BigDecimal.valueOf(3000);

    private final OrdenRepository ordenRepository;
    private final ProductoRepository productoRepository;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Transactional
    public OrdenResponseDTO crearOrden(OrdenRequestDTO dto) throws MPException, MPApiException {
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new IllegalArgumentException("La orden debe tener al menos un item");
        }

        List<OrdenItem> items = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (OrdenItemRequestDTO itemDto : dto.getItems()) {
            Producto producto = productoRepository.findById(itemDto.getProductoId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Producto no encontrado: " + itemDto.getProductoId()));

            BigDecimal precioUnitario = BigDecimal.valueOf(producto.getPrecio());
            BigDecimal subtotalItem = precioUnitario.multiply(BigDecimal.valueOf(itemDto.getCantidad()));

            OrdenItem item = OrdenItem.builder()
                    .productoId(producto.getId())
                    .nombreProducto(producto.getNombre())
                    .imagenUrl(producto.getImagenUrl())
                    .cantidad(itemDto.getCantidad())
                    .precioUnitario(precioUnitario.doubleValue())
                    .build();

            items.add(item);
            subtotal = subtotal.add(subtotalItem);
        }

        BigDecimal envio = subtotal.compareTo(UMBRAL_ENVIO_GRATIS) < 0
                ? COSTO_ENVIO_FIJO
                : BigDecimal.ZERO;
        BigDecimal total = subtotal.add(envio);

        Orden orden = Orden.builder()
                .nombreComprador(dto.getNombreComprador())
                .emailComprador(dto.getEmailComprador())
                .telefonoComprador(dto.getTelefonoComprador())
                .direccionEnvio(dto.getDireccionEnvio())
                .ciudadEnvio(dto.getCiudadEnvio())
                .provinciaEnvio(dto.getProvinciaEnvio())
                .costoEnvio(envio)
                .total(total)
                .items(items)
                .build();

        items.forEach(item -> item.setOrden(orden));

        Orden ordenGuardada = ordenRepository.save(orden);

        List<PreferenceItemRequest> mpItems = items.stream().map(item ->
                PreferenceItemRequest.builder()
                        .id(String.valueOf(item.getProductoId()))
                        .title(item.getNombreProducto())
                        .pictureUrl(item.getImagenUrl())
                        .quantity(item.getCantidad())
                        .unitPrice(BigDecimal.valueOf(item.getPrecioUnitario()))
                        .currencyId("ARS")
                        .build()
        ).collect(Collectors.toCollection(ArrayList::new));

        if (envio.compareTo(BigDecimal.ZERO) > 0) {
            mpItems.add(
                    PreferenceItemRequest.builder()
                            .id("envio")
                            .title("Costo de envio")
                            .quantity(1)
                            .unitPrice(envio)
                            .currencyId("ARS")
                            .build()
            );
        }

        PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                .success(frontendUrl + "/checkout/exitoso")
                .failure(frontendUrl + "/checkout/fallido")
                .pending(frontendUrl + "/checkout/pendiente")
                .build();

        PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                .items(mpItems)
                .backUrls(backUrls)
                .externalReference(String.valueOf(ordenGuardada.getId()))
                .build();

        PreferenceClient client = new PreferenceClient();
        Preference preference = client.create(preferenceRequest);

        ordenGuardada.setMpPreferenceId(preference.getId());
        ordenRepository.save(ordenGuardada);

        return toResponseDTO(ordenGuardada, preference.getSandboxInitPoint());
    }

    public OrdenResponseDTO buscarPorId(Long id) {
        Orden orden = ordenRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Orden no encontrada: " + id));
        return toResponseDTO(orden, null);
    }

    public Page<OrdenResponseDTO> listarPaginado(Pageable pageable) {
        return ordenRepository.findAllByOrderByFechaCreacionDesc(pageable)
                .map(orden -> toResponseDTO(orden, null));
    }

    private OrdenResponseDTO toResponseDTO(Orden orden, String initPoint) {
        List<OrdenItemDTO> itemDTOs = orden.getItems().stream().map(item ->
                OrdenItemDTO.builder()
                        .productoId(item.getProductoId())
                        .nombreProducto(item.getNombreProducto())
                        .imagenUrl(item.getImagenUrl())
                        .cantidad(item.getCantidad())
                        .precioUnitario(item.getPrecioUnitario())
                        .subtotal(item.getPrecioUnitario() * item.getCantidad())
                        .build()
        ).collect(Collectors.toList());

        return OrdenResponseDTO.builder()
                .id(orden.getId())
                .nombreComprador(orden.getNombreComprador())
                .emailComprador(orden.getEmailComprador())
                .total(orden.getTotal() != null ? orden.getTotal().doubleValue() : null)
                .estado(orden.getEstado())
                .mpPreferenceId(orden.getMpPreferenceId())
                .initPoint(initPoint)
                .items(itemDTOs)
                .fechaCreacion(orden.getFechaCreacion())
                .build();
    }
}
