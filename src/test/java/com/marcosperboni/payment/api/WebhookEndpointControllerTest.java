package com.marcosperboni.payment.api;

import com.marcosperboni.payment.domain.exception.WebhookEndpointNotFoundException;
import com.marcosperboni.payment.domain.model.WebhookEndpoint;
import com.marcosperboni.payment.security.JwtAuthenticationFilter;
import com.marcosperboni.payment.service.WebhookEndpointService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WebhookEndpointController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class WebhookEndpointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WebhookEndpointService webhookEndpointService;

    @Test
    void createRejectsBlankUrl() throws Exception {
        String body = """
                {"merchantReference": "merchant-1", "url": ""}
                """;

        mockMvc.perform(post("/api/v1/webhook-endpoints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRejectsNonHttpUrl() throws Exception {
        String body = """
                {"merchantReference": "merchant-1", "url": "ftp://example.com"}
                """;

        mockMvc.perform(post("/api/v1/webhook-endpoints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createReturns201OnValidRequest() throws Exception {
        WebhookEndpoint endpoint = new WebhookEndpoint("merchant-1", "https://example.com/hook");
        when(webhookEndpointService.register(eq("merchant-1"), eq("https://example.com/hook")))
                .thenReturn(endpoint);

        String body = """
                {"merchantReference": "merchant-1", "url": "https://example.com/hook"}
                """;

        mockMvc.perform(post("/api/v1/webhook-endpoints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void listReturnsAllEndpoints() throws Exception {
        when(webhookEndpointService.listAll()).thenReturn(
                List.of(new WebhookEndpoint("merchant-1", "https://example.com/hook")));

        mockMvc.perform(get("/api/v1/webhook-endpoints"))
                .andExpect(status().isOk());
    }

    @Test
    void updateRejectsInvalidUrl() throws Exception {
        UUID id = UUID.randomUUID();
        String body = """
                {"url": "not-a-url"}
                """;

        mockMvc.perform(put("/api/v1/webhook-endpoints/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateReturns404WhenMissing() throws Exception {
        UUID id = UUID.randomUUID();
        when(webhookEndpointService.updateUrl(eq(id), any())).thenThrow(new WebhookEndpointNotFoundException(id));

        String body = """
                {"url": "https://example.com/new-hook"}
                """;

        mockMvc.perform(put("/api/v1/webhook-endpoints/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteReturns204OnSuccess() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/webhook-endpoints/{id}", id))
                .andExpect(status().isNoContent());

        verify(webhookEndpointService).delete(id);
    }
}
