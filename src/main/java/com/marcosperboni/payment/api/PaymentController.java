package com.marcosperboni.payment.api;

import com.marcosperboni.payment.api.dto.CreatePaymentRequest;
import com.marcosperboni.payment.api.dto.PaymentOperationResponse;
import com.marcosperboni.payment.api.dto.PaymentResponse;
import com.marcosperboni.payment.domain.model.Payment;
import com.marcosperboni.payment.domain.model.PaymentStatus;
import com.marcosperboni.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Currency;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@Validated
@Tag(name = "Payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> create(
            @RequestHeader("Idempotency-Key") @NotBlank(message = "Idempotency-Key header is required") String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request) {

        Payment payment = paymentService.createPayment(
                request.amount(), Currency.getInstance(request.currency()), request.method(),
                request.merchantReference(), idempotencyKey);

        return ResponseEntity.status(HttpStatus.CREATED).body(PaymentResponse.from(payment));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(PaymentResponse.from(paymentService.getPayment(id)));
    }

    @GetMapping
    public ResponseEntity<Page<PaymentResponse>> list(
            @RequestParam(required = false) PaymentStatus status, Pageable pageable) {
        return ResponseEntity.ok(paymentService.listPayments(status, pageable).map(PaymentResponse::from));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<PaymentOperationResponse>> history(@PathVariable UUID id) {
        List<PaymentOperationResponse> history = paymentService.getHistory(id).stream()
                .map(PaymentOperationResponse::from)
                .toList();
        return ResponseEntity.ok(history);
    }

    @PostMapping("/{id}/capture")
    public ResponseEntity<PaymentResponse> capture(@PathVariable UUID id) {
        return ResponseEntity.ok(PaymentResponse.from(paymentService.capturePayment(id)));
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<PaymentResponse> refund(@PathVariable UUID id) {
        return ResponseEntity.ok(PaymentResponse.from(paymentService.refundPayment(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<PaymentResponse> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(PaymentResponse.from(paymentService.cancelPayment(id)));
    }
}
