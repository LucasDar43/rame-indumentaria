package com.tudominio.rame_indumentaria.service;

import com.mercadopago.client.merchantorder.MerchantOrderClient;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.merchantorder.MerchantOrder;
import com.mercadopago.resources.payment.Payment;
import com.tudominio.rame_indumentaria.model.EstadoOrden;
import com.tudominio.rame_indumentaria.model.Orden;
import com.tudominio.rame_indumentaria.repository.OrdenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.mercadopago.client.merchantorder.MerchantOrderClient;
import com.mercadopago.resources.merchantorder.MerchantOrder;
import com.mercadopago.resources.merchantorder.MerchantOrderPayment;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final OrdenRepository ordenRepository;

    @Transactional
    public void procesarWebhook(Map<String, Object> payload) {
        String type = (String) payload.get("type");
        String topic = (String) payload.get("topic");

        log.info("🔥 Webhook recibido - type: {} topic: {}", type, topic);

        try {
            // 🔹 Caso 1: payment
            if ("payment".equals(type)) {
                procesarPayment(payload);
                return;
            }

            // 🔹 Caso 2: merchant_order
            if ("merchant_order".equals(topic)) {
                String resource = (String) payload.get("resource");

                if (resource == null) {
                    log.error("merchant_order sin resource");
                    return;
                }

                procesarMerchantOrder(resource);
                return;
            }

            log.info("Webhook ignorado");

        } catch (Exception e) {
            log.error("Error procesando webhook", e);
        }
    }

    // =========================
    // PAYMENT FLOW
    // =========================

    private void procesarPayment(Map<String, Object> payload) throws MPException, MPApiException {
        Map<?, ?> data = (Map<?, ?>) payload.get("data");

        if (data == null || data.get("id") == null) {
            log.error("Webhook payment sin data o id");
            return;
        }

        Long paymentId = Long.parseLong(String.valueOf(data.get("id")));

        log.info("Consultando payment {}", paymentId);

        PaymentClient paymentClient = new PaymentClient();
        Payment payment = paymentClient.get(paymentId);

        actualizarOrdenDesdePayment(payment);
        log.info("Payment completo: {}", payment);
    }

    // =========================
    // MERCHANT ORDER FLOW (CLAVE)
    // =========================

    private void procesarMerchantOrder(String resourceUrl) {

        try {
            // =========================
            // EXTRAER ID DE LA URL
            // =========================
            String[] parts = resourceUrl.split("/");
            Long merchantOrderId = Long.parseLong(parts[parts.length - 1]);

            log.info("Consultando merchant_order {}", merchantOrderId);

            // =========================
            // CONSULTAR A MP
            // =========================
            MerchantOrderClient client = new MerchantOrderClient();
            MerchantOrder merchantOrder = client.get(merchantOrderId);

            // =========================
            // LOGS COMPLETOS
            // =========================
            log.info("===== MERCHANT ORDER RAW =====");
            log.info("ID: {}", merchantOrder.getId());
            log.info("Status: {}", merchantOrder.getOrderStatus());
            log.info("Total amount: {}", merchantOrder.getTotalAmount());
            log.info("Paid amount: {}", merchantOrder.getPaidAmount());
            log.info("Payments: {}", merchantOrder.getPayments());
            log.info("================================");

            // =========================
            // VALIDAR PAYMENTS
            // =========================
            var payments = merchantOrder.getPayments();

            if (payments == null || payments.isEmpty()) {
                log.warn("merchant_order sin payments asociados");
                return;
            }

            log.info("Cantidad de payments: {}", payments.size());

            // =========================
            // RECORRER PAYMENTS
            // =========================
            for (MerchantOrderPayment payment : payments) {

                log.info("---- PAYMENT ----");
                log.info("Payment ID: {}", payment.getId());
                log.info("Status: {}", payment.getStatus());
                log.info("Total Paid Amount: {}", payment.getTotalPaidAmount());
                log.info("------------------");

                // ⚠️ SOLO PROCESAR APROBADOS
                if (!"approved".equals(payment.getStatus())) {
                    continue;
                }

                // =========================
                // CONSULTAR PAYMENT COMPLETO
                // =========================
                PaymentClient paymentClient = new PaymentClient();
                Payment mpPayment = paymentClient.get(payment.getId());

                String externalReference = mpPayment.getExternalReference();

                if (externalReference == null || externalReference.isBlank()) {
                    log.error("Payment {} sin externalReference", payment.getId());
                    continue;
                }

                // =========================
                // ACTUALIZAR ORDEN
                // =========================
                Orden orden = ordenRepository.findById(Long.parseLong(externalReference))
                        .orElseThrow(() -> new RuntimeException("Orden no encontrada: " + externalReference));

                orden.setMpPaymentId(String.valueOf(payment.getId()));
                orden.setEstado(EstadoOrden.APROBADO);

                ordenRepository.save(orden);

                log.info("Orden {} actualizada a APROBADO desde merchant_order", orden.getId());
            }

        } catch (Exception e) {
            log.error("Error procesando merchant_order", e);
        }
    }

    // =========================
    // CORE UPDATE
    // =========================

    private void actualizarOrdenDesdePayment(Payment payment) {
        try {
            String externalReference = payment.getExternalReference();
            String mpStatus = payment.getStatus();
            String paymentId = String.valueOf(payment.getId());

            log.info("Payment {} - status: {} - externalReference: {}", paymentId, mpStatus, externalReference);

            if (externalReference == null || externalReference.isBlank()) {
                log.error("Payment sin externalReference");
                return;
            }

            Orden orden = ordenRepository.findById(Long.parseLong(externalReference))
                    .orElseThrow(() -> new RuntimeException("Orden no encontrada: " + externalReference));

            // 🧠 Idempotencia básica
            if (paymentId.equals(orden.getMpPaymentId()) &&
                    orden.getEstado() == mapearEstado(mpStatus)) {

                log.info("Orden {} ya estaba actualizada, se ignora", orden.getId());
                return;
            }

            orden.setMpPaymentId(paymentId);
            orden.setEstado(mapearEstado(mpStatus));

            ordenRepository.save(orden);

            log.info("Orden {} actualizada a estado {}", orden.getId(), orden.getEstado());

        } catch (Exception e) {
            log.error("Error actualizando orden desde payment", e);
        }
    }

    // =========================
    // STATUS MAP
    // =========================

    private EstadoOrden mapearEstado(String mpStatus) {
        return switch (mpStatus) {
            case "approved" -> EstadoOrden.APROBADO;
            case "rejected" -> EstadoOrden.RECHAZADO;
            case "cancelled" -> EstadoOrden.CANCELADO;
            default -> EstadoOrden.PENDIENTE;
        };
    }
}
