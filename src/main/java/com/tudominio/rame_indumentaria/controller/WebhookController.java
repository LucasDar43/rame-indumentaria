package com.tudominio.rame_indumentaria.controller;

import com.tudominio.rame_indumentaria.service.WebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping("/mercadopago")
    public ResponseEntity<Void> recibirWebhook(
            @RequestHeader("x-signature") String xSignature,
            @RequestHeader(value = "x-request-id", required = false) String xRequestId,
            @RequestBody Map<String, Object> payload) {

        System.out.println("ðŸ”¥ WEBHOOK RECIBIDO:");
        System.out.println(payload);

        webhookService.validarFirma(xSignature, xRequestId, payload);
        webhookService.procesarWebhook(payload);
        return ResponseEntity.ok().build();
    }
}
