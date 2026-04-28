package com.tudominio.rame_indumentaria.service;

import com.mercadopago.client.merchantorder.MerchantOrderClient;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.merchantorder.MerchantOrder;
import com.mercadopago.resources.merchantorder.MerchantOrderPayment;
import com.mercadopago.resources.payment.Payment;
import com.tudominio.rame_indumentaria.model.EstadoOrden;
import com.tudominio.rame_indumentaria.model.Orden;
import com.tudominio.rame_indumentaria.repository.OrdenRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final OrdenRepository ordenRepository;

    @Value("${mercadopago.webhook-secret}")
    private String webhookSecret;

    @Value("${app.modo-test:false}")
    private boolean modoTest;

    @Transactional
    @SneakyThrows
    public void procesarWebhook(Map<String, Object> payload) {
        String type = (String) payload.get("type");
        String topic = (String) payload.get("topic");

        log.info("ðŸ”¥ Webhook recibido - type: {} topic: {}", type, topic);

        // ðŸ”¹ Caso 1: payment
        if ("payment".equals(type)) {
            procesarPayment(payload);
            return;
        }

        // ðŸ”¹ Caso 2: merchant_order
        if ("merchant_order".equals(topic)) {
            String resource = (String) payload.get("resource");
            procesarMerchantOrder(resource);
            return;
        }

        log.info("Webhook ignorado");

    }

    public void validarFirma(String xSignature, String xRequestId, Map<String, Object> payload) {
        if (xSignature == null || xSignature.isBlank()) {
            throw new SecurityException("Header x-signature ausente");
        }

        String ts = null;
        String v1 = null;

        for (String part : xSignature.split(",")) {
            String trimmedPart = part.trim();

            if (trimmedPart.startsWith("ts=")) {
                ts = trimmedPart.substring(3);
            } else if (trimmedPart.startsWith("v1=")) {
                v1 = trimmedPart.substring(3);
            }
        }

        if (ts == null || v1 == null) {
            throw new SecurityException("Formato de x-signature inválido");
        }

        long tsValue;
        try {
            tsValue = Long.parseLong(ts);
        } catch (NumberFormatException ex) {
            throw new SecurityException("Formato de x-signature inválido");
        }

        if (Math.abs(System.currentTimeMillis() / 1000 - tsValue) > 300) {
            throw new SecurityException("Webhook expirado");
        }

        Object rawData = payload.get("data");
        if (!(rawData instanceof Map<?, ?>)) {
            throw new SecurityException("payload.data.id ausente");
        }

        Object rawId = ((Map<?, ?>) rawData).get("id");
        if (rawId == null) {
            throw new SecurityException("payload.data.id ausente");
        }

        String dataId = String.valueOf(rawId);
        String stringFirmado = "id:" + dataId + ";request-id:" + (xRequestId != null ? xRequestId : "") + ";ts:" + ts + ";";
        String computedHash = generarHmacSHA256(stringFirmado, webhookSecret);

        if (!MessageDigest.isEqual(
                computedHash.getBytes(StandardCharsets.UTF_8),
                v1.trim().getBytes(StandardCharsets.UTF_8))) {
            throw new SecurityException("Firma de webhook inválida");
        }
    }

    // =========================
    // PAYMENT FLOW
    // =========================

    private void procesarPayment(Map<String, Object> payload) throws MPException, MPApiException {
        if (modoTest) {
            Map<?, ?> data = (Map<?, ?>) payload.get("data");
            if (data == null || data.get("id") == null) {
                throw new RuntimeException("Payload inválido en modo test");
            }
            String paymentId = String.valueOf(data.get("id"));
            log.info("Modo test: simulando aprobación para orden {}", paymentId);
            Orden orden = ordenRepository.findById(Long.parseLong(paymentId))
                    .orElseThrow(() -> new RuntimeException("Orden no encontrada en modo test: " + paymentId));
            orden.setEstado(EstadoOrden.APROBADO);
            orden.setMpPaymentId(paymentId);
            ordenRepository.save(orden);
            return;
        }

        Map<?, ?> data = (Map<?, ?>) payload.get("data");

        if (data == null || data.get("id") == null) {
            log.error("Webhook payment sin data o id");
            return;
        }

        Long paymentId = Long.parseLong(String.valueOf(data.get("id")));
        String paymentIdStr = String.valueOf(paymentId);

        log.info("Consultando payment {}", paymentId);

        PaymentClient paymentClient = new PaymentClient();
        if (ordenRepository.findByMpPaymentId(paymentIdStr).isPresent()) {
            log.info("Webhook duplicado ignorado, paymentId: {}", paymentIdStr);
            return;
        }

        Payment payment = paymentClient.get(paymentId);

        actualizarOrdenDesdePayment(payment);
        log.info("Payment completo: {}", payment);
    }

    // =========================
    // MERCHANT ORDER FLOW (CLAVE)
    // =========================

    @SneakyThrows
    private void procesarMerchantOrder(String resourceUrl) {

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

            // âš ï¸ SOLO PROCESAR APROBADOS
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

    }

    // =========================
    // CORE UPDATE
    // =========================

    private void actualizarOrdenDesdePayment(Payment payment) {
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

        if ("approved".equals(mpStatus) && orden.getTotal() != null) {
            BigDecimal montoPagado = payment.getTransactionAmount();

            if (montoPagado == null) {
                throw new RuntimeException(
                        "Payment " + paymentId + " no tiene transactionAmount"
                );
            }

            // Tolerancia de $1 ARS para diferencias de redondeo
            BigDecimal diferencia = orden.getTotal()
                    .subtract(montoPagado)
                    .abs();

            if (diferencia.compareTo(BigDecimal.ONE) > 0) {
                throw new RuntimeException(
                        "Monto pagado " + montoPagado +
                                " no coincide con total de orden " + orden.getTotal() +
                                " para orden " + orden.getId()
                );
            }
        }

        if (orden.getMpPaymentId() != null &&
                orden.getMpPaymentId().equals(paymentId) &&
                orden.getEstado() == mapearEstado(mpStatus)) {

            log.info("Webhook duplicado ignorado, paymentId: {}", paymentId);
            return;
        }

        orden.setMpPaymentId(paymentId);
        orden.setEstado(mapearEstado(mpStatus));

        ordenRepository.save(orden);

        log.info("Orden {} actualizada a estado {}", orden.getId(), orden.getEstado());

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

    private String generarHmacSHA256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);

            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                hexString.append(String.format("%02x", b & 0xff));
            }

            return hexString.toString();
        } catch (Exception e) {
            log.error("Error al calcular HMAC: {}", e.getMessage());
            throw new RuntimeException("Error interno al validar firma");
        }
    }
}
