package com.tudominio.rame_indumentaria.service;

import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.resources.payment.Payment;
import com.tudominio.rame_indumentaria.model.EstadoOrden;
import com.tudominio.rame_indumentaria.model.Orden;
import com.tudominio.rame_indumentaria.repository.OrdenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebhookServiceProcesarWebhookTest {

    @Mock
    private OrdenRepository ordenRepository;

    private WebhookService webhookService;

    @BeforeEach
    void setUp() {
        webhookService = new WebhookService(ordenRepository);
    }

    @Test
    void procesarWebhookActualizaOrdenAAprobadoCuandoPaymentEsApproved() throws Exception {
        Orden orden = orden(10L, null, EstadoOrden.PENDIENTE);
        Payment payment = paymentMock(123L, "approved", "10");

        when(ordenRepository.findByMpPaymentId("123")).thenReturn(Optional.empty());
        when(ordenRepository.findById(10L)).thenReturn(Optional.of(orden));
        when(ordenRepository.save(any(Orden.class))).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedConstruction<PaymentClient> mocked = mockConstruction(
                PaymentClient.class,
                (paymentClientMock, context) -> when(paymentClientMock.get(123L)).thenReturn(payment)
        )) {
            webhookService.procesarWebhook(payloadPayment("123"));

            assertThat(mocked.constructed()).hasSize(1);
        }

        ArgumentCaptor<Orden> captor = ArgumentCaptor.forClass(Orden.class);
        verify(ordenRepository).save(captor.capture());

        Orden saved = captor.getValue();
        assertThat(saved.getMpPaymentId()).isEqualTo("123");
        assertThat(saved.getEstado()).isEqualTo(EstadoOrden.APROBADO);
    }

    @Test
    void procesarWebhookActualizaOrdenARechazadoCuandoPaymentEsRejected() throws Exception {
        Orden orden = orden(10L, null, EstadoOrden.PENDIENTE);
        Payment payment = paymentMock(456L, "rejected", "10");

        when(ordenRepository.findByMpPaymentId("456")).thenReturn(Optional.empty());
        when(ordenRepository.findById(10L)).thenReturn(Optional.of(orden));
        when(ordenRepository.save(any(Orden.class))).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedConstruction<PaymentClient> mocked = mockConstruction(
                PaymentClient.class,
                (paymentClientMock, context) -> when(paymentClientMock.get(456L)).thenReturn(payment)
        )) {
            webhookService.procesarWebhook(payloadPayment("456"));

            assertThat(mocked.constructed()).hasSize(1);
        }

        ArgumentCaptor<Orden> captor = ArgumentCaptor.forClass(Orden.class);
        verify(ordenRepository).save(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoOrden.RECHAZADO);
        assertThat(captor.getValue().getMpPaymentId()).isEqualTo("456");
    }

    @Test
    void procesarWebhookIgnoraWebhookRetryCuandoYaExisteMpPaymentIdAntesDeConsultarPayment() throws Exception {
        Orden ordenExistente = orden(10L, "123", EstadoOrden.APROBADO);
        when(ordenRepository.findByMpPaymentId("123")).thenReturn(Optional.of(ordenExistente));

        try (MockedConstruction<PaymentClient> mocked = mockConstruction(PaymentClient.class)) {
            webhookService.procesarWebhook(payloadPayment("123"));

            assertThat(mocked.constructed()).hasSize(1);
            verify(mocked.constructed().get(0), never()).get(any(Long.class));
        }

        verify(ordenRepository, never()).findById(any());
        verify(ordenRepository, never()).save(any());
    }

    @Test
    void procesarWebhookIgnoraActualizacionCuandoLaOrdenYaTieneMismoPaymentIdYMismoEstado() throws Exception {
        Orden orden = orden(10L, "123", EstadoOrden.APROBADO);
        Payment payment = paymentMock(123L, "approved", "10");

        when(ordenRepository.findByMpPaymentId("123")).thenReturn(Optional.empty());
        when(ordenRepository.findById(10L)).thenReturn(Optional.of(orden));

        try (MockedConstruction<PaymentClient> mocked = mockConstruction(
                PaymentClient.class,
                (paymentClientMock, context) -> when(paymentClientMock.get(123L)).thenReturn(payment)
        )) {
            webhookService.procesarWebhook(payloadPayment("123"));

            verify(mocked.constructed().get(0)).get(123L);
        }

        verify(ordenRepository, never()).save(any());
    }

    @Test
    void procesarWebhookDosVecesSoloGuardaUnaActualizacion() throws Exception {
        Orden orden = orden(10L, null, EstadoOrden.PENDIENTE);
        Payment payment = paymentMock(777L, "approved", "10");

        when(ordenRepository.findByMpPaymentId("777"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(orden));
        when(ordenRepository.findById(10L)).thenReturn(Optional.of(orden));
        when(ordenRepository.save(any(Orden.class))).thenAnswer(invocation -> {
            Orden saved = invocation.getArgument(0);
            orden.setMpPaymentId(saved.getMpPaymentId());
            orden.setEstado(saved.getEstado());
            return saved;
        });

        try (MockedConstruction<PaymentClient> mocked = mockConstruction(
                PaymentClient.class,
                (paymentClientMock, context) -> when(paymentClientMock.get(777L)).thenReturn(payment)
        )) {
            webhookService.procesarWebhook(payloadPayment("777"));
            webhookService.procesarWebhook(payloadPayment("777"));

            assertThat(mocked.constructed()).hasSize(2);
            verify(mocked.constructed().get(0)).get(777L);
            verify(mocked.constructed().get(1), never()).get(any(Long.class));
        }

        verify(ordenRepository, times(1)).save(any(Orden.class));
    }

    private Map<String, Object> payloadPayment(String paymentId) {
        return Map.of(
                "type", "payment",
                "data", Map.of("id", paymentId)
        );
    }

    private Payment paymentMock(Long paymentId, String status, String externalReference) {
        return paymentMock(paymentId, status, externalReference, null);
    }

    private Payment paymentMock(Long paymentId, String status, String externalReference,
                                java.math.BigDecimal transactionAmount) {
        Payment payment = mock(Payment.class);
        when(payment.getId()).thenReturn(paymentId);
        when(payment.getStatus()).thenReturn(status);
        when(payment.getExternalReference()).thenReturn(externalReference);
        lenient().when(payment.getTransactionAmount()).thenReturn(transactionAmount);
        return payment;
    }

    private Orden orden(Long id, String mpPaymentId, EstadoOrden estado) {
        return Orden.builder()
                .id(id)
                .mpPaymentId(mpPaymentId)
                .estado(estado)
                .build();
    }
}
