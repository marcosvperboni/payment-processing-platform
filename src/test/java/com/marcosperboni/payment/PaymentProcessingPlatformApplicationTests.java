package com.marcosperboni.payment;

import com.marcosperboni.payment.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentProcessingPlatformApplicationTests extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
    }
}
