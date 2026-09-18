package com.example.refunds;

/**
 * A refund request as it arrives: any member may be missing, since checking it is the
 * service's job.
 *
 * @param destination {@code card} or {@code bank-account}
 * @param amount      the amount, in cents
 * @param cardLast4   the card's last four digits, for a refund to a card
 * @param iban        the account, for a refund to a bank account
 */
public record RefundRequest(String destination, Integer amount, String cardLast4, String iban) {
}
