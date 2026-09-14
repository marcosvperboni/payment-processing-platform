package com.marcosperboni.payment.api;

import com.marcosperboni.payment.domain.exception.InvalidPaymentStateException;
import com.marcosperboni.payment.domain.exception.PaymentNotFoundException;
import com.marcosperboni.payment.domain.model.Payment;
import com.marcosperboni.payment.domain.model.PaymentMethod;
import com.marcosperboni.payment.domain.model.PaymentStatus;
import com.marcosperboni.payment.security.JwtAuthenticationFilter;
import com.marcosperboni.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PaymentController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    void createRejectsMissingIdempotencyKey() throws Exception {
        String body = """
                {"amount": 10.00, "currency": "USD", "method": "PIX"}
                """;

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRejectsNegativeAmount() throws Exception {
        String body = """
                {"amount": -5.00, "currency": "USD", "method": "PIX"}
                """;

        mockMvc.perform(post("/api/v1/payments")
                        .header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]").value(org.hamcrest.Matchers.containsString("amount")));
    }

    @Test
    void createRejectsInvalidCurrencyFormat() throws Exception {
        String body = """
                {"amount": 10.00, "currency": "us", "method": "PIX"}
                """;

        mockMvc.perform(post("/api/v1/payments")
                        .header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRejectsUnknownMethod() throws Exception {
        String body = """
                {"amount": 10.00, "currency": "USD", "method": "CASH"}
                """;

        mockMvc.perform(post("/api/v1/payments")
                        .header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createReturns201OnValidRequest() throws Exception {
        Payment payment = Payment.create(new BigDecimal("10.00"), Currency.getInstance("USD"),
                PaymentMethod.PIX, null, "key-1");
        when(paymentService.createPayment(eq(new BigDecimal("10.00")), eq(Currency.getInstance("USD")),
                eq(PaymentMethod.PIX), any(), eq("key-1"))).thenReturn(payment);

        String body = """
                {"amount": 10.00, "currency": "USD", "method": "PIX"}
                """;

        mockMvc.perform(post("/api/v1/payments")
                        .header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void getReturns404WhenPaymentMissing() throws Exception {
        UUID id = UUID.randomUUID();
        when(paymentService.getPayment(id)).thenThrow(new PaymentNotFoundException(id));

        mockMvc.perform(get("/api/v1/payments/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void captureReturns409OnInvalidTransition() throws Exception {
        UUID id = UUID.randomUUID();
        when(paymentService.capturePayment(id))
                .thenThrow(new InvalidPaymentStateException(PaymentStatus.CREATED, "capture"));

        mockMvc.perform(post("/api/v1/payments/{id}/capture", id))
                .andExpect(status().isConflict());
    }

    @Test
    void createRejectsMalformedUuidPathVariableOnGet() throws Exception {
        mockMvc.perform(get("/api/v1/payments/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest());
    }
}
