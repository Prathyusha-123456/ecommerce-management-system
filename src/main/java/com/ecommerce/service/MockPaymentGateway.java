package com.ecommerce.service;

import com.ecommerce.entity.PaymentMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Simulated gateway for development and demos - no real money moves.
 * Approves everything except amounts above the test limit, which lets you exercise the failure path.
 */
@Component
public class MockPaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(MockPaymentGateway.class);
    private static final BigDecimal TEST_LIMIT = new BigDecimal("1000000");

    @Override
    public Result charge(BigDecimal amount, PaymentMethod method, String reference) {
        if (amount.compareTo(TEST_LIMIT) > 0) {
            return new Result(false, null, "amount exceeds the test gateway limit of " + TEST_LIMIT);
        }
        String txnId = "TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        log.info("[MockGateway] Charged {} via {} for {} -> {}", amount, method, reference, txnId);
        return new Result(true, txnId, "approved");
    }

    @Override
    public void refund(String transactionId, BigDecimal amount) {
        log.info("[MockGateway] Refunded {} for {}", amount, transactionId);
    }
}
