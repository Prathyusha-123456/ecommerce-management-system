package com.ecommerce.exception;

/** Thrown inside the checkout transaction, so the whole order (and its stock changes) is rolled back. */
public class PaymentFailedException extends RuntimeException {
    public PaymentFailedException(String message) {
        super(message);
    }
}
