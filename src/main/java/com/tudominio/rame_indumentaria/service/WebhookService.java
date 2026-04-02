package com.tudominio.rame_indumentaria.service;

import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.tudominio.rame_indumentaria.model.EstadoOrden;
import com.tudominio.rame_indumentaria.model.Orden;
import com.tudominio.rame_indumentaria.repository.OrdenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final OrdenRepository ordenRepository;

    @Transactional
    public void procesarWebhook(Map<String, Object> payload) {
        String type = (String) payload.get("type");

        // Solo nos interesan los eventos de pago
        if (!"payment".equals(type)) {
            log.info("Webhook ignorado, tipo: {}", type);
            return;
        }

        try {
            Map<?, ?> data = (Map<?, ?>) payload.get("data");
            String paymentIdStr = String.valueOf(data.get("id"));
            Long paymentId = Long.parseLong(paymentIdStr);

            // Consultar el pago a MP para obtener el estado real
            PaymentClient paymentClient = new PaymentClient();
            Payment payment = paymentClient.get(paymentId);

            String externalReference = payment.getExternalReference(); // es el id de nuestra orden
            String mpStatus = payment.getStatus();

            log.info("Webhook payment {} - estado: {} - orden: {}", paymentId, mpStatus, externalReference);

            Orden orden = ordenRepository.findById(Long.parseLong(externalReference))
                    .orElseThrow(() -> new RuntimeException("Orden no encontrada: " + externalReference));

            orden.setMpPaymentId(paymentIdStr);
            orden.setEstado(mapearEstado(mpStatus));
            ordenRepository.save(orden);

        } catch (MPException | MPApiException e) {
            log.error("Error al consultar pago a MercadoPago: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Error inesperado en webhook: {}", e.getMessage());
        }
    }

    private EstadoOrden mapearEstado(String mpStatus) {
        return switch (mpStatus) {
            case "approved" -> EstadoOrden.APROBADO;
            case "rejected" -> EstadoOrden.RECHAZADO;
            case "cancelled" -> EstadoOrden.CANCELADO;
            default -> EstadoOrden.PENDIENTE;
        };
    }
}
