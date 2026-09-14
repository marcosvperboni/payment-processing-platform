package com.marcosperboni.payment.integration;

import com.marcosperboni.payment.api.dto.CreatePaymentRequest;
import com.marcosperboni.payment.api.dto.LoginRequest;
import com.marcosperboni.payment.api.dto.LoginResponse;
import com.marcosperboni.payment.api.dto.PaymentOperationResponse;
import com.marcosperboni.payment.api.dto.PaymentResponse;
import com.marcosperboni.payment.api.dto.WebhookEndpointRequest;
import com.marcosperboni.payment.api.dto.WebhookEndpointResponse;
import com.marcosperboni.payment.domain.model.OperationType;
import com.marcosperboni.payment.domain.model.PaymentMethod;
import com.marcosperboni.payment.domain.model.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentFlowIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    private final TestRestTemplate restTemplate = new TestRestTemplate();

    private String baseUrl;
    private String merchantToken;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        merchantToken = login("merchant", "merchant123");
    }

    @Test
    void fullLifecycleGoesFromCreatedToRefunded() {
        PaymentResponse created = createPayment(new BigDecimal("100.00"), "order-" + System.nanoTime());
        assertThat(created.status()).isEqualTo(PaymentStatus.PROCESSING);

        PaymentResponse approved = awaitStatus(created.id(), PaymentStatus.APPROVED);
        assertThat(approved.gatewayReference()).isNotBlank();

        PaymentResponse captured = post("/api/v1/payments/" + created.id() + "/capture", null, PaymentResponse.class);
        assertThat(captured.status()).isEqualTo(PaymentStatus.SETTLED);

        PaymentResponse refunded = post("/api/v1/payments/" + created.id() + "/refund", null, PaymentResponse.class);
        assertThat(refunded.status()).isEqualTo(PaymentStatus.REFUNDED);

        List<PaymentOperationResponse> history = getHistory(created.id());
        assertThat(history).extracting(PaymentOperationResponse::type).containsExactly(
                OperationType.CREATED, OperationType.AUTHORIZATION_REQUESTED, OperationType.APPROVED,
                OperationType.CAPTURED, OperationType.REFUNDED);
    }

    @Test
    void decliningAmountEndsAsDeclined() {
        PaymentResponse created = createPayment(new BigDecimal("13.13"), "order-decline-" + System.nanoTime());

        PaymentResponse declined = awaitStatus(created.id(), PaymentStatus.DECLINED);

        assertThat(declined.gatewayReference()).isNotBlank();
    }

    @Test
    void sameIdempotencyKeyReturnsSamePayment() {
        String idempotencyKey = "idem-" + System.nanoTime();
        PaymentResponse first = createPayment(new BigDecimal("42.00"), "order-a", idempotencyKey);
        PaymentResponse second = createPayment(new BigDecimal("42.00"), "order-a", idempotencyKey);

        assertThat(second.id()).isEqualTo(first.id());
    }

    @Test
    void creatingWithoutIdempotencyKeyIsRejected() {
        HttpHeaders headers = authHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var request = new CreatePaymentRequest(new BigDecimal("10.00"), "USD", PaymentMethod.PIX, null);

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/api/v1/payments", new HttpEntity<>(request, headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void requestWithoutTokenIsUnauthorized() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Idempotency-Key", "no-auth-key");
        var request = new CreatePaymentRequest(new BigDecimal("10.00"), "USD", PaymentMethod.PIX, null);

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/api/v1/payments", new HttpEntity<>(request, headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void webhookEndpointCrudLifecycle() {
        HttpHeaders headers = authHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var createRequest = new WebhookEndpointRequest("merchant-x", "https://example.com/hook");

        ResponseEntity<WebhookEndpointResponse> createResponse = restTemplate.postForEntity(
                baseUrl + "/api/v1/webhook-endpoints", new HttpEntity<>(createRequest, headers),
                WebhookEndpointResponse.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var endpointId = createResponse.getBody().id();

        restTemplate.exchange(baseUrl + "/api/v1/webhook-endpoints/" + endpointId, HttpMethod.DELETE,
                new HttpEntity<>(headers), Void.class);

        ResponseEntity<String> getAfterDelete = restTemplate.exchange(
                baseUrl + "/api/v1/webhook-endpoints/" + endpointId, HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
        assertThat(getAfterDelete.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void undeliverableWebhookEndsUpAsFailedAfterRetries() {
        HttpHeaders headers = authHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var createRequest = new WebhookEndpointRequest("merchant-unreachable", "http://127.0.0.1:1/never");
        restTemplate.postForEntity(baseUrl + "/api/v1/webhook-endpoints",
                new HttpEntity<>(createRequest, headers), WebhookEndpointResponse.class);

        PaymentResponse created = createPayment(new BigDecimal("77.00"), "order-webhook-" + System.nanoTime());
        awaitStatus(created.id(), PaymentStatus.APPROVED);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            List<PaymentOperationResponse> history = getHistory(created.id());
            assertThat(history).extracting(PaymentOperationResponse::type).contains(OperationType.WEBHOOK_FAILED);
        });
    }

    private PaymentResponse awaitStatus(java.util.UUID paymentId, PaymentStatus expected) {
        return await().atMost(Duration.ofSeconds(10))
                .until(() -> get("/api/v1/payments/" + paymentId, PaymentResponse.class),
                        payment -> payment.status() == expected);
    }

    private PaymentResponse createPayment(BigDecimal amount, String merchantReference) {
        return createPayment(amount, merchantReference, "idem-" + System.nanoTime());
    }

    private PaymentResponse createPayment(BigDecimal amount, String merchantReference, String idempotencyKey) {
        HttpHeaders headers = authHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Idempotency-Key", idempotencyKey);
        var request = new CreatePaymentRequest(amount, "USD", PaymentMethod.PIX, merchantReference);

        ResponseEntity<PaymentResponse> response = restTemplate.postForEntity(
                baseUrl + "/api/v1/payments", new HttpEntity<>(request, headers), PaymentResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private List<PaymentOperationResponse> getHistory(java.util.UUID paymentId) {
        HttpHeaders headers = authHeaders();
        ResponseEntity<PaymentOperationResponse[]> response = restTemplate.exchange(
                baseUrl + "/api/v1/payments/" + paymentId + "/history", HttpMethod.GET,
                new HttpEntity<>(headers), PaymentOperationResponse[].class);
        return List.of(response.getBody());
    }

    private <T> T get(String path, Class<T> type) {
        ResponseEntity<T> response = restTemplate.exchange(baseUrl + path, HttpMethod.GET,
                new HttpEntity<>(authHeaders()), type);
        return response.getBody();
    }

    private <T> T post(String path, Object body, Class<T> type) {
        HttpHeaders headers = authHeaders();
        ResponseEntity<T> response = restTemplate.postForEntity(baseUrl + path, new HttpEntity<>(body, headers), type);
        return response.getBody();
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(merchantToken);
        return headers;
    }

    private String login(String username, String password) {
        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                baseUrl + "/api/v1/auth/login", new LoginRequest(username, password), LoginResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody().token();
    }
}
