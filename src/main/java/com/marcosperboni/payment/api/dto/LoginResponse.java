package com.marcosperboni.payment.api.dto;

public record LoginResponse(String token, long expiresInSeconds) {
}
