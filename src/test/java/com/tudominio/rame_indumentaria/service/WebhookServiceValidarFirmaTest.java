package com.tudominio.rame_indumentaria.service;

import com.tudominio.rame_indumentaria.repository.OrdenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class WebhookServiceValidarFirmaTest {

    private static final String WEBHOOK_SECRET = "unit-test-webhook-secret";
    private static final String REQUEST_ID = "request-test-123";

    @Mock
    private OrdenRepository ordenRepository;

    private WebhookService webhookService;

    @BeforeEach
    void setUp() {
        webhookService = new WebhookService(ordenRepository);
        ReflectionTestUtils.setField(webhookService, "webhookSecret", WEBHOOK_SECRET);
    }

    @Test
    void validarFirmaNoLanzaExcepcionCuandoLaFirmaEsValida() {
        Map<String, Object> payload = payloadConDataId("123456");
        String signature = generarSignature("123456", REQUEST_ID, nowEpochSeconds(), WEBHOOK_SECRET);

        assertThatNoException()
                .isThrownBy(() -> webhookService.validarFirma(signature, REQUEST_ID, payload));
    }

    @Test
    void validarFirmaLanzaSecurityExceptionCuandoLaFirmaEsInvalida() {
        Map<String, Object> payload = payloadConDataId("123456");
        String signature = generarSignature("123456", REQUEST_ID, nowEpochSeconds(), "otro-secret");

        assertThatThrownBy(() -> webhookService.validarFirma(signature, REQUEST_ID, payload))
                .isInstanceOf(SecurityException.class)
                .hasMessage("Firma de webhook inválida");
    }

    @Test
    void validarFirmaLanzaSecurityExceptionCuandoXSignatureEsNull() {
        assertThatThrownBy(() -> webhookService.validarFirma(null, REQUEST_ID, payloadConDataId("123456")))
                .isInstanceOf(SecurityException.class)
                .hasMessage("Header x-signature ausente");
    }

    @Test
    void validarFirmaLanzaSecurityExceptionCuandoElFormatoEsInvalido() {
        assertThatThrownBy(() -> webhookService.validarFirma("ts=123456789", REQUEST_ID, payloadConDataId("123456")))
                .isInstanceOf(SecurityException.class)
                .hasMessage("Formato de x-signature inválido");
    }

    @Test
    void validarFirmaLanzaSecurityExceptionCuandoElTimestampEstaFueraDeVentana() {
        long expiredTs = nowEpochSeconds() - 301;
        String signature = generarSignature("123456", REQUEST_ID, expiredTs, WEBHOOK_SECRET);

        assertThatThrownBy(() -> webhookService.validarFirma(signature, REQUEST_ID, payloadConDataId("123456")))
                .isInstanceOf(SecurityException.class)
                .hasMessage("Webhook expirado");
    }

    @Test
    void validarFirmaLanzaSecurityExceptionCuandoFaltaDataId() {
        Map<String, Object> payload = Map.of("type", "payment", "data", Map.of());
        String signature = generarSignature("123456", REQUEST_ID, nowEpochSeconds(), WEBHOOK_SECRET);

        assertThatThrownBy(() -> webhookService.validarFirma(signature, REQUEST_ID, payload))
                .isInstanceOf(SecurityException.class)
                .hasMessage("payload.data.id ausente");
    }

    @Test
    void validarFirmaLanzaSecurityExceptionCuandoFaltaData() {
        Map<String, Object> payload = Map.of("type", "payment");
        String signature = generarSignature("123456", REQUEST_ID, nowEpochSeconds(), WEBHOOK_SECRET);

        assertThatThrownBy(() -> webhookService.validarFirma(signature, REQUEST_ID, payload))
                .isInstanceOf(SecurityException.class)
                .hasMessage("payload.data.id ausente");
    }

    @Test
    void validarFirmaLanzaSecurityExceptionCuandoDataNoEsMap() {
        Map<String, Object> payload = Map.of("type", "payment", "data", "texto");
        String signature = generarSignature("123456", REQUEST_ID, nowEpochSeconds(), WEBHOOK_SECRET);

        assertThatThrownBy(() -> webhookService.validarFirma(signature, REQUEST_ID, payload))
                .isInstanceOf(SecurityException.class)
                .hasMessage("payload.data.id ausente");
    }

    @Test
    void validarFirmaLanzaSecurityExceptionCuandoTsNoEsNumerico() {
        String signature = "ts=abc,v1=" + generarHmac("id:123456;request-id:" + REQUEST_ID + ";ts:abc;", WEBHOOK_SECRET);

        assertThatThrownBy(() -> webhookService.validarFirma(signature, REQUEST_ID, payloadConDataId("123456")))
                .isInstanceOf(SecurityException.class)
                .hasMessage("Formato de x-signature inválido");
    }

    @Test
    void validarFirmaLanzaSecurityExceptionCuandoLaSignatureEstaMalformada() {
        String signature = "v1-sin-igualdad";

        assertThatThrownBy(() -> webhookService.validarFirma(signature, REQUEST_ID, payloadConDataId("123456")))
                .isInstanceOf(SecurityException.class)
                .hasMessage("Formato de x-signature inválido");
    }

    private Map<String, Object> payloadConDataId(String dataId) {
        return Map.of(
                "type", "payment",
                "data", Map.of("id", dataId)
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
}
