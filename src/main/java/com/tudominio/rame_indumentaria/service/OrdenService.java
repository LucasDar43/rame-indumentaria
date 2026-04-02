package com.tudominio.rame_indumentaria.service;

import com.mercadopago.client.preference.*;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.preference.Preference;
import com.tudominio.rame_indumentaria.dto.*;
import com.tudominio.rame_indumentaria.model.*;
import com.tudominio.rame_indumentaria.repository.OrdenRepository;
import com.tudominio.rame_indumentaria.repository.ProductoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrdenService {

    private final OrdenRepository ordenRepository;
    private final ProductoRepository productoRepository;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Transactional
    public OrdenResponseDTO crearOrden(OrdenRequestDTO dto) throws MPException, MPApiException {

        // 1. Construir items y calcular total
        List<OrdenItem> items = new ArrayList<>();
        double total = 0;

        for (OrdenItemRequestDTO itemDto : dto.getItems()) {
            Producto producto = productoRepository.findById(itemDto.getProductoId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Producto no encontrado: " + itemDto.getProductoId()));

            OrdenItem item = OrdenItem.builder()
                    .productoId(producto.getId())
                    .nombreProducto(producto.getNombre())
                    .imagenUrl(producto.getImagenUrl())
                    .cantidad(itemDto.getCantidad())
                    .precioUnitario(producto.getPrecio())
                    .build();

            items.add(item);
            total += producto.getPrecio() * itemDto.getCantidad();
        }

        // 2. Guardar la orden con estado PENDIENTE (sin preferenceId aún)
        Orden orden = Orden.builder()
                .nombreComprador(dto.getNombreComprador())
                .emailComprador(dto.getEmailComprador())
                .telefonoComprador(dto.getTelefonoComprador())
                .direccionEnvio(dto.getDireccionEnvio())
                .ciudadEnvio(dto.getCiudadEnvio())
                .provinciaEnvio(dto.getProvinciaEnvio())
                .total(total)
                .items(items)
                .build();

        // Asociar items a la orden
        items.forEach(item -> item.setOrden(orden));

        Orden ordenGuardada = ordenRepository.save(orden);

        // 3. Crear preferencia en MercadoPago
        List<PreferenceItemRequest> mpItems = items.stream().map(item ->
                PreferenceItemRequest.builder()
                        .id(String.valueOf(item.getProductoId()))
                        .title(item.getNombreProducto())
                        .pictureUrl(item.getImagenUrl())
                        .quantity(item.getCantidad())
                        .unitPrice(BigDecimal.valueOf(item.getPrecioUnitario()))
                        .currencyId("ARS")
                        .build()
        ).collect(Collectors.toList());

        PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                .success(frontendUrl + "/checkout/exitoso")
                .failure(frontendUrl + "/checkout/fallido")
                .pending(frontendUrl + "/checkout/pendiente")
                .build();

        PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                .items(mpItems)
                .backUrls(backUrls)
                //.autoReturn("approved")
                .externalReference(String.valueOf(ordenGuardada.getId()))
                .build();

        PreferenceClient client = new PreferenceClient();
        Preference preference = client.create(preferenceRequest);

        // 4. Actualizar la orden con el preferenceId
        ordenGuardada.setMpPreferenceId(preference.getId());
        ordenRepository.save(ordenGuardada);

        // 5. Responder con init_point para redirigir al cliente
        return toResponseDTO(ordenGuardada, preference.getSandboxInitPoint());
    }

    public OrdenResponseDTO buscarPorId(Long id) {
        Orden orden = ordenRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Orden no encontrada: " + id));
        return toResponseDTO(orden, null);
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
                .total(orden.getTotal())
                .estado(orden.getEstado())
                .mpPreferenceId(orden.getMpPreferenceId())
                .initPoint(initPoint)
                .items(itemDTOs)
                .fechaCreacion(orden.getFechaCreacion())
                .build();
    }
}
