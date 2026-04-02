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
    public ResponseEntity<Void> recibirWebhook(@RequestBody Map<String, Object> payload) {
        webhookService.procesarWebhook(payload);
        return ResponseEntity.ok().build();
    }
}