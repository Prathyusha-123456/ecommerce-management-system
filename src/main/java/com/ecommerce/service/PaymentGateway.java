package com.ecommerce.service;

import com.ecommerce.entity.PaymentMethod;

import java.math.BigDecimal;

/**
 * Abstraction over a payment provider. The app only depends on this interface, so a real
 * provider (Razorpay, Stripe, ...) can replace {@link MockPaymentGateway} without touching the order logic.
 */
public interface PaymentGateway {

    Result charge(BigDecimal amount, PaymentMethod method, String reference);

    void refund(String transactionId, BigDecimal amount);

    record Result(boolean success, String transactionId, String message) {
    }
}
