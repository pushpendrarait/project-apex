package com.projectapex.controller;

import com.projectapex.dto.response.ApiResponse;
import com.projectapex.service.WebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class WebhookController {
    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    /**
     * POST /api/v1/webhooks/razorpay - Asynchronous Razorpay Webhook Handler
     * Strictly reads raw payload string to ensure HMAC-SHA256 signature validity
     */
    @PostMapping(value = "/webhooks/razorpay", consumes = {"application/json", "text/plain", "*/*"})
    public ResponseEntity<ApiResponse<String>> handleRazorpayWebhook(
            @RequestBody String rawPayload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signatureHeader,
            @RequestHeader(value = "X-Razorpay-Event-Id", required = false) String eventId
    ) {
        if (signatureHeader == null) {
            log.warn("Missing X-Razorpay-Signature header in webhook request");
            return ResponseEntity.badRequest().body(ApiResponse.error("Missing X-Razorpay-Signature"));
        }

        boolean processed = webhookService.processWebhook(rawPayload, signatureHeader, eventId);
        return ResponseEntity.ok(ApiResponse.ok("Webhook processed successfully", "Event acknowledged"));
    }
}
