package com.tudominio.rame_indumentaria.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.resources.payment.Payment;
import com.tudominio.rame_indumentaria.model.EstadoOrden;
import com.tudominio.rame_indumentaria.model.Orden;
import com.tudominio.rame_indumentaria.repository.OrdenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebhookControllerIntegrationTest {

    private static final String WEBHOOK_SECRET = "test-webhook";
    private static final String REQUEST_ID = "req-int-123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrdenRepository ordenRepository;

    @BeforeEach
    void setUp() {
        ordenRepository.deleteAll();
    }

    @Test
    void webhookValidoRetornaOk() throws Exception {
        Orden orden = ordenRepository.save(Orden.builder()
                .nombreComprador("Webhook OK")
                .emailComprador("ok@test.com")
                .estado(EstadoOrden.PENDIENTE)
                .build());

        Map<String, Object> payload = payloadPayment("1001");
        String signature = generarSignature("1001", REQUEST_ID, nowEpochSeconds(), WEBHOOK_SECRET);
        Payment payment = paymentMock(1001L, "approved", String.valueOf(orden.getId()));

        try (MockedConstruction<PaymentClient> mocked = mockConstruction(
                PaymentClient.class,
                (paymentClientMock, context) -> when(paymentClientMock.get(1001L)).thenReturn(payment)
        )) {
            mockMvc.perform(post("/api/webhook/mercadopago")
                            .contentType(APPLICATION_JSON)
                            .header("x-signature", signature)
                            .header("x-request-id", REQUEST_ID)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isOk());

            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    void webhookConFirmaInvalidaRetornaUnauthorized() throws Exception {
        Map<String, Object> payload = payloadPayment("1002");

        mockMvc.perform(post("/api/webhook/mercadopago")
                        .contentType(APPLICATION_JSON)
                        .header("x-signature", "ts=" + nowEpochSeconds() + ",v1=firma-invalida")
                        .header("x-request-id", REQUEST_ID)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void webhookConHeaderDeFirmaVacioRetornaUnauthorized() throws Exception {
        Map<String, Object> payload = payloadPayment("1003");

        mockMvc.perform(post("/api/webhook/mercadopago")
                        .contentType(APPLICATION_JSON)
                        .header("x-signature", "")
                        .header("x-request-id", REQUEST_ID)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void webhookValidoActualizaOrden() throws Exception {
        Orden orden = ordenRepository.save(Orden.builder()
                .nombreComprador("Webhook Update")
                .emailComprador("update@test.com")
                .estado(EstadoOrden.PENDIENTE)
                .build());

        Map<String, Object> payload = payloadPayment("2001");
        String signature = generarSignature("2001", REQUEST_ID, nowEpochSeconds(), WEBHOOK_SECRET);
        Payment payment = paymentMock(2001L, "approved", String.valueOf(orden.getId()));

        try (MockedConstruction<PaymentClient> mocked = mockConstruction(
                PaymentClient.class,
                (paymentClientMock, context) -> when(paymentClientMock.get(2001L)).thenReturn(payment)
        )) {
            mockMvc.perform(post("/api/webhook/mercadopago")
                            .contentType(APPLICATION_JSON)
                            .header("x-signature", signature)
                            .header("x-request-id", REQUEST_ID)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isOk());

            verify(mocked.constructed().get(0)).get(2001L);
        }

        Orden actualizada = ordenRepository.findById(orden.getId()).orElseThrow();
        assertThat(actualizada.getMpPaymentId()).isEqualTo("2001");
        assertThat(actualizada.getEstado()).isEqualTo(EstadoOrden.APROBADO);
    }

    @Test
    void mismoWebhookDosVecesSoloActualizaLaOrdenUnaVez() throws Exception {
        Orden orden = ordenRepository.save(Orden.builder()
                .nombreComprador("Webhook Retry")
                .emailComprador("retry@test.com")
                .estado(EstadoOrden.PENDIENTE)
                .build());

        Map<String, Object> payload = payloadPayment("3001");
        String signature = generarSignature("3001", REQUEST_ID, nowEpochSeconds(), WEBHOOK_SECRET);
        Payment payment = paymentMock(3001L, "approved", String.valueOf(orden.getId()));

        try (MockedConstruction<PaymentClient> mocked = mockConstruction(
                PaymentClient.class,
                (paymentClientMock, context) -> when(paymentClientMock.get(3001L)).thenReturn(payment)
        )) {
            mockMvc.perform(post("/api/webhook/mercadopago")
                            .contentType(APPLICATION_JSON)
                            .header("x-signature", signature)
                            .header("x-request-id", REQUEST_ID)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/webhook/mercadopago")
                            .contentType(APPLICATION_JSON)
                            .header("x-signature", signature)
                            .header("x-request-id", REQUEST_ID)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isOk());

            assertThat(mocked.constructed()).hasSize(2);
            verify(mocked.constructed().get(0)).get(3001L);
            verify(mocked.constructed().get(1), org.mockito.Mockito.never()).get(any(Long.class));
        }

        assertThat(ordenRepository.findAll()).hasSize(1);
        Orden actualizada = ordenRepository.findById(orden.getId()).orElseThrow();
        assertThat(actualizada.getMpPaymentId()).isEqualTo("3001");
        assertThat(actualizada.getEstado()).isEqualTo(EstadoOrden.APROBADO);
    }

    private Map<String, Object> payloadPayment(String paymentId) {
        return Map.of(
                "type", "payment",
                "data", Map.of("id", paymentId)
        );
    }

    private long nowEpochSeconds() {
        return Instant.now().getEpochSecond();
    }

    private String generarSignature(String dataId, String requestId, long ts, String secret) {
        String manifest = "id:" + dataId + ";request-id:" + requestId + ";ts:" + ts + ";";
        return "ts=" + ts + ",v1=" + generarHmac(manifest, secret);
    }

    private String generarHmac(String data, String secret) {
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
            throw new RuntimeException(e);
        }
    }

    private Payment paymentMock(Long paymentId, String status, String externalReference) {
        Payment payment = mock(Payment.class);
        when(payment.getId()).thenReturn(paymentId);
        when(payment.getStatus()).thenReturn(status);
        when(payment.getExternalReference()).thenReturn(externalReference);
        return payment;
    }
}
