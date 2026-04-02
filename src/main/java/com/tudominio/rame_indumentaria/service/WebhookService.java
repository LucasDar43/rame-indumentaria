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
        log.info("Webhook recibido - tipo: {}", type);

        // Solo nos interesan los eventos de pago
        if (!"payment".equals(type)) {
            log.info("Webhook ignorado, tipo no es 'payment': {}", type);
            return;
        }

        try {
            Map<?, ?> data = (Map<?, ?>) payload.get("data");
            if (data == null || data.get("id") == null) {
                log.error("Webhook payment sin data o id");
                return;
            }
            String paymentIdStr = String.valueOf(data.get("id"));
            Long paymentId = Long.parseLong(paymentIdStr);

            log.info("Consultando pago {} a MercadoPago...", paymentId);
            PaymentClient paymentClient = new PaymentClient();
            Payment payment = paymentClient.get(paymentId);

            String externalReference = payment.getExternalReference();
            String mpStatus = payment.getStatus();
            String mpStatusDetail = payment.getStatusDetail();

            log.info("Payment MP {} - status: {} ({}) - externalReference: {}",
                    paymentId, mpStatus, mpStatusDetail, externalReference);

            if (externalReference == null || externalReference.isBlank()) {
                log.error("Payment {} no tiene externalReference. No se puede vincular a orden.", paymentId);
                return;
            }

            Orden orden = ordenRepository.findById(Long.parseLong(externalReference))
                    .orElseThrow(() -> new RuntimeException("Orden no encontrada: " + externalReference));

            orden.setMpPaymentId(paymentIdStr);
            orden.setEstado(mapearEstado(mpStatus));
            ordenRepository.save(orden);
            log.info("Orden {} actualizada a estado {}", orden.getId(), orden.getEstado());

        } catch (MPApiException e) {
            log.error("MP Status Code: {}", e.getStatusCode());
            log.error("MP Response: {}", e.getApiResponse().getContent());
        } catch (MPException e) {
            log.error("MP Exception: {}", e.getMessage());
        } catch (NumberFormatException e) {
            log.error("Error de formato numérico en externalReference: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Error inesperado en webhook: {}", e.getMessage(), e);
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
