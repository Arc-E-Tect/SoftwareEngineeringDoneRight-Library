package com.example.refunds;

/**
 * A refund the shop accepted.
 *
 * @param id          its identifier
 * @param orderId     the order it is for
 * @param amount      the amount, in cents
 * @param destination where the money goes
 */
public record Refund(String id, String orderId, int amount, String destination) {
}
