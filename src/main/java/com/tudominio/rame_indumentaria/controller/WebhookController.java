package com.tudominio.rame_indumentaria.controller;

import com.tudominio.rame_indumentaria.service.WebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;

    @Value("${app.modo-test:false}")
    private boolean modoTest;

    @PostMapping("/mercadopago")
    public ResponseEntity<Void> recibirWebhook(
            @RequestHeader(value = "x-signature", required = false) String xSignature,
            @RequestHeader(value = "x-request-id", required = false) String xRequestId,
            @RequestBody Map<String, Object> payload) {

        System.out.println("ðŸ”¥ WEBHOOK RECIBIDO:");
        System.out.println(payload);

        if (!modoTest) {
            webhookService.validarFirma(xSignature, xRequestId, payload);
        }
        webhookService.procesarWebhook(payload);
        return ResponseEntity.ok().build();
    }
}
