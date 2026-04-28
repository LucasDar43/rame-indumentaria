package com.tudominio.rame_indumentaria.service;

import com.mercadopago.client.preference.*;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.preference.Preference;
import com.tudominio.rame_indumentaria.dto.OrdenItemDTO;
import com.tudominio.rame_indumentaria.dto.OrdenItemRequestDTO;
import com.tudominio.rame_indumentaria.dto.OrdenPublicaDTO;
import com.tudominio.rame_indumentaria.dto.OrdenRequestDTO;
import com.tudominio.rame_indumentaria.dto.OrdenResponseDTO;
import com.tudominio.rame_indumentaria.model.Orden;
import com.tudominio.rame_indumentaria.model.OrdenItem;
import com.tudominio.rame_indumentaria.model.Producto;
import com.tudominio.rame_indumentaria.model.Variante;
import com.tudominio.rame_indumentaria.repository.OrdenRepository;
import com.tudominio.rame_indumentaria.repository.ProductoRepository;
import com.tudominio.rame_indumentaria.repository.VarianteRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrdenService {

    private static final BigDecimal UMBRAL_ENVIO_GRATIS = BigDecimal.valueOf(50000);
    private static final BigDecimal COSTO_ENVIO_FIJO = BigDecimal.valueOf(3000);

    private final OrdenRepository ordenRepository;
    private final ProductoRepository productoRepository;
    private final VarianteRepository varianteRepository;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    @Value("${app.modo-test:false}")
    private boolean modoTest;

    @Transactional
    public OrdenResponseDTO crearOrden(OrdenRequestDTO dto) throws MPException, MPApiException {

        log.info("🚀 INICIO crearOrden");

        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new IllegalArgumentException("La orden debe tener al menos un item");
        }

        List<OrdenItem> items = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        // =========================
        // ARMADO DE ITEMS
        // =========================
        for (OrdenItemRequestDTO itemDto : dto.getItems()) {

            Producto producto = productoRepository.findById(itemDto.getProductoId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Producto no encontrado: " + itemDto.getProductoId()));

            Long varianteId = itemDto.getVarianteId();
            Variante variante = varianteRepository.findById(varianteId)
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Variante no encontrada: " + varianteId));

            if (!Boolean.TRUE.equals(variante.getActivo())) {
                throw new IllegalArgumentException("Variante no disponible: " + varianteId);
            }

            if (!variante.getProducto().getId().equals(itemDto.getProductoId())) {
                throw new IllegalArgumentException("Variante no pertenece al producto indicado");
            }

            if (variante.getStock() < itemDto.getCantidad()) {
                throw new IllegalArgumentException("Stock insuficiente para: " + producto.getNombre()
                        + " - Talle: " + variante.getTalle()
                        + " - Color: " + variante.getColor());
            }

            variante.setStock(variante.getStock() - itemDto.getCantidad());
            varianteRepository.save(variante);

            BigDecimal precioUnitario = producto.getPrecio();
            BigDecimal subtotalItem = precioUnitario.multiply(BigDecimal.valueOf(itemDto.getCantidad()));

            log.info("🧾 Item DTO → id: {} nombre: {} precio: {} cantidad: {}",
                    producto.getId(),
                    producto.getNombre(),
                    producto.getPrecio(),
                    itemDto.getCantidad()
            );

            OrdenItem item = OrdenItem.builder()
                    .productoId(producto.getId())
                    .varianteId(variante.getId())
                    .nombreProducto(producto.getNombre())
                    .talle(variante.getTalle())
                    .color(variante.getColor())
                    .imagenUrl(producto.getImagenUrl())
                    .cantidad(itemDto.getCantidad())
                    .precioUnitario(precioUnitario)
                    .build();

            items.add(item);
            subtotal = subtotal.add(subtotalItem);
        }

        BigDecimal envio = modoTest
                ? BigDecimal.ZERO
                : (subtotal.compareTo(UMBRAL_ENVIO_GRATIS) < 0
                ? COSTO_ENVIO_FIJO
                : BigDecimal.ZERO);

        BigDecimal total = subtotal.add(envio);

        log.info("💰 Subtotal: {}", subtotal);
        log.info("🚚 Envío: {}", envio);
        log.info("💳 Total: {}", total);

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

        log.info("🧾 Orden guardada ID: {}", ordenGuardada.getId());

        // =========================
        // ITEMS MP
        // =========================
        List<PreferenceItemRequest> mpItems = items.stream().map(item -> {

            log.info("🧾 Item MP → nombre: {} precio: {} cantidad: {}",
                    item.getNombreProducto(),
                    item.getPrecioUnitario(),
                    item.getCantidad()
            );

            return PreferenceItemRequest.builder()
                    .id(String.valueOf(item.getProductoId()))
                    .title(item.getNombreProducto() != null ? item.getNombreProducto() : "Producto")
                    .pictureUrl("https://via.placeholder.com/150")
                    .quantity(item.getCantidad())
                    .unitPrice(item.getPrecioUnitario())
                    .currencyId("ARS")
                    .build();

        }).collect(Collectors.toCollection(ArrayList::new));

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

        log.info("🛒 Total items enviados a MP: {}", mpItems.size());

        String frontendBaseUrl = frontendUrl.endsWith("/")
                ? frontendUrl.substring(0, frontendUrl.length() - 1)
                : frontendUrl;
        String backendBaseUrl = appBaseUrl.endsWith("/")
                ? appBaseUrl.substring(0, appBaseUrl.length() - 1)
                : appBaseUrl;

        // =========================
        // BACK URLS
        // =========================
        PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                .success(frontendBaseUrl + "/checkout/exitoso")
                .failure(frontendBaseUrl + "/checkout/fallido")
                .pending(frontendBaseUrl + "/checkout/pendiente")
                .build();

        log.info("🌐 Back URLs: {}", backUrls);

        // =========================
        // PREFERENCE
        // =========================
        PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                .items(mpItems)
                .backUrls(backUrls)
                .notificationUrl(backendBaseUrl + "/api/webhook/mercadopago")
                .externalReference(String.valueOf(ordenGuardada.getId()))
                .paymentMethods(
                        PreferencePaymentMethodsRequest.builder()
                                .installments(1)
                                .defaultInstallments(1)
                                .build()
                )
                .expires(false)
                .build();

        // =========================
        // CREAR PREFERENCE (DEBUG FUERTE)
        // =========================
        PreferenceClient client = new PreferenceClient();

        try {
            log.info("🚀 Enviando preference a MercadoPago...");
            log.info("ExternalReference: {}", ordenGuardada.getId());
            log.info("Frontend URL: {}", frontendUrl);

            Preference preference = client.create(preferenceRequest);

            log.info("✅ Preference creada correctamente: {}", preference.getId());

            ordenGuardada.setMpPreferenceId(preference.getId());
            ordenRepository.save(ordenGuardada);

            return toResponseDTO(ordenGuardada, preference.getInitPoint());

        } catch (MPApiException e) {

            log.error("❌ ERROR MPApiException");
            log.error("Status Code: {}", e.getStatusCode());

            if (e.getApiResponse() != null) {
                log.error("Response Content: {}", e.getApiResponse().getContent());
            }

            throw e;

        } catch (MPException e) {

            log.error("❌ ERROR MPException");
            log.error("Message: {}", e.getMessage());
            throw e;

        } catch (Exception e) {

            log.error("❌ ERROR GENERAL creando preference", e);
            throw e;
        }
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

    public OrdenPublicaDTO buscarPublicoPorId(Long id) {
        Orden orden = ordenRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Orden no encontrada: " + id));
        return OrdenPublicaDTO.builder()
                .id(orden.getId())
                .estado(orden.getEstado())
                .mpPreferenceId(orden.getMpPreferenceId())
                .build();
    }

    private OrdenResponseDTO toResponseDTO(Orden orden, String initPoint) {
        List<OrdenItemDTO> itemDTOs = orden.getItems().stream().map(item ->
                OrdenItemDTO.builder()
                        .productoId(item.getProductoId())
                        .varianteId(item.getVarianteId())
                        .nombreProducto(item.getNombreProducto())
                        .talle(item.getTalle())
                        .color(item.getColor())
                        .imagenUrl(item.getImagenUrl())
                        .cantidad(item.getCantidad())
                        .precioUnitario(item.getPrecioUnitario())
                        .subtotal(item.getPrecioUnitario()
                                .multiply(BigDecimal.valueOf(item.getCantidad())))
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
