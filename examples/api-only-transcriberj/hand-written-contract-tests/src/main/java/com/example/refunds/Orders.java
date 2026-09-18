package com.example.refunds;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The orders a refund can be for, and how much of each is refunded so far.
 * Two orders, in memory: enough for an example.
 */
@Component
public class Orders {

    /** An order of 5000 cents, paid by card. */
    public static final String PAID_BY_CARD = "order-paid-by-card";

    /** An order of 5000 cents, paid by bank transfer. */
    public static final String PAID_BY_BANK_TRANSFER = "order-paid-by-bank-transfer";

    /**
     * An order.
     *
     * @param id       its identifier
     * @param paidWith how it was paid: {@code card} or {@code bank-account}
     * @param total    what it cost, in cents
     * @param refunded how much of that is refunded so far
     */
    public record Order(String id, String paidWith, int total, int refunded) {

        /**
         * What is left to refund.
         *
         * @return the amount, in cents
         */
        public int left() {
            return total - refunded;
        }
    }

    private final Map<String, Order> orders = new ConcurrentHashMap<>(Map.of(
            PAID_BY_CARD, new Order(PAID_BY_CARD, "card", 5000, 0),
            PAID_BY_BANK_TRANSFER, new Order(PAID_BY_BANK_TRANSFER, "bank-account", 5000, 0)));

    /**
     * An order, if there is one with that identifier.
     *
     * @param id the identifier
     * @return the order
     */
    public Optional<Order> find(String id) {
        return Optional.ofNullable(orders.get(id));
    }

    /**
     * Records a refund against an order.
     *
     * @param id     the order's identifier
     * @param amount the amount refunded, in cents
     */
    public void refund(String id, int amount) {
        orders.computeIfPresent(id, (key, order) ->
                new Order(order.id(), order.paidWith(), order.total(), order.refunded() + amount));
    }
}
