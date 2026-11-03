package com.ecommerce.service;

import com.ecommerce.entity.Order;
import com.ecommerce.entity.Payment;
import com.ecommerce.entity.PaymentMethod;
import com.ecommerce.entity.PaymentStatus;
import com.ecommerce.exception.PaymentFailedException;
import com.ecommerce.repository.PaymentRepository;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    private final PaymentGateway gateway;
    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentGateway gateway, PaymentRepository paymentRepository) {
        this.gateway = gateway;
        this.paymentRepository = paymentRepository;
    }

    /**
     * Runs inside the caller's (checkout) transaction. If the gateway declines, the exception rolls
     * back the order and the stock deduction together, so nothing is half-saved.
     */
    public Payment process(Order order, PaymentMethod method) {
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(order.getTotalAmount());
        payment.setMethod(method);

        if (method == PaymentMethod.COD) {
            payment.setStatus(PaymentStatus.PENDING); // collected on delivery
            payment.setTransactionId("COD-" + order.getId());
        } else {
            PaymentGateway.Result result = gateway.charge(order.getTotalAmount(), method, "ORDER-" + order.getId());
            if (!result.success()) {
                throw new PaymentFailedException("Payment failed: " + result.message());
            }
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setTransactionId(result.transactionId());
        }
        return paymentRepository.save(payment);
    }

    /** Called when an order is cancelled. */
    public void refundIfNeeded(Order order) {
        Payment payment = order.getPayment();
        if (payment == null) {
            return;
        }
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            if (payment.getMethod() != PaymentMethod.COD) {
                gateway.refund(payment.getTransactionId(), payment.getAmount());
            }
            payment.setStatus(PaymentStatus.REFUNDED);
        } else if (payment.getStatus() == PaymentStatus.PENDING) {
            payment.setStatus(PaymentStatus.CANCELLED);
        }
    }
}
