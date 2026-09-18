package com.example.refunds;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What the service does with a refund request: which ones it refuses, and why. These are
 * scenarios, one per rule, and they are the place for every way a request can be wrong --
 * {@link CreateRefundContractTest} needs only one of them to check the shape of the answer.
 */
class RefundsTest {

    private final Refunds refunds = new Refunds(new Orders());

    private static RefundRequest toCard(Integer amount, String cardLast4) {
        return new RefundRequest("card", amount, cardLast4, null);
    }

    private static RefundRequest toBankAccount(Integer amount, String iban) {
        return new RefundRequest("bank-account", amount, null, iban);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', nullValues = "null", textBlock = """
            no amount            | card   | null    | 4242 | null               | amount    | is required
            a zero amount        | card   | 0       | 4242 | null               | amount    | must be between 1 and 1000000
            too large an amount  | card   | 1000001 | 4242 | null               | amount    | must be between 1 and 1000000
            no card digits       | card   | 100     | null | null               | cardLast4 | must be the last four digits of the card
            three card digits    | card   | 100     | 424  | null               | cardLast4 | must be the last four digits of the card
            letters for digits   | card   | 100     | abcd | null               | cardLast4 | must be the last four digits of the card
            no IBAN              | bank-account | 100 | null | null             | iban      | must be an IBAN without spaces
            an IBAN with spaces  | bank-account | 100 | null | NL91 ABNA 0417 1643 00 | iban | must be an IBAN without spaces
            no destination       | null   | 100     | 4242 | null               | destination | must be card or bank-account
            an unknown destination | cash | 100     | 4242 | null               | destination | must be card or bank-account
            """)
    void aRequestTheContractDoesNotAllowIsInvalid(String scenario, String destination, Integer amount,
                                                  String cardLast4, String iban, String field, String message) {
        Outcome outcome = refunds.refund(Orders.PAID_BY_CARD, new RefundRequest(destination, amount, cardLast4, iban));

        assertThat(outcome).isEqualTo(new Outcome.Invalid(List.of(new FieldError(field, message))));
    }

    @Test
    void everyProblemWithARequestIsReportedAtOnce() {
        Outcome outcome = refunds.refund(Orders.PAID_BY_CARD, toCard(0, "abcd"));

        assertThat(outcome).isEqualTo(new Outcome.Invalid(List.of(
                new FieldError("amount", "must be between 1 and 1000000"),
                new FieldError("cardLast4", "must be the last four digits of the card"))));
    }

    @Test
    void aRefundGoesBackTheWayTheOrderWasPaid() {
        assertThat(refunds.refund(Orders.PAID_BY_CARD, toBankAccount(100, "NL91ABNA0417164300")))
                .isEqualTo(new Outcome.Rejected("The order was paid by card, so its refunds go back to the card."));
        assertThat(refunds.refund(Orders.PAID_BY_BANK_TRANSFER, toCard(100, "4242")))
                .isEqualTo(new Outcome.Rejected(
                        "The order was paid by bank-account, so its refunds go back to the bank-account."));
    }

    @Test
    void refundsNeverAddUpToMoreThanTheOrder() {
        assertThat(refunds.refund(Orders.PAID_BY_CARD, toCard(3000, "4242"))).isInstanceOf(Outcome.Accepted.class);

        assertThat(refunds.refund(Orders.PAID_BY_CARD, toCard(2500, "4242")))
                .isEqualTo(new Outcome.Rejected("Only 2000 of the order is left to refund."));
        assertThat(refunds.refund(Orders.PAID_BY_CARD, toCard(2000, "4242"))).isInstanceOf(Outcome.Accepted.class);
    }

    @Test
    void anAcceptedRefundSaysWhatWasRefundedAndWhere() {
        Outcome outcome = refunds.refund(Orders.PAID_BY_BANK_TRANSFER, toBankAccount(1250, "NL91ABNA0417164300"));

        assertThat(outcome).isInstanceOfSatisfying(Outcome.Accepted.class, accepted -> {
            assertThat(accepted.refund().id()).isNotBlank();
            assertThat(accepted.refund().orderId()).isEqualTo(Orders.PAID_BY_BANK_TRANSFER);
            assertThat(accepted.refund().amount()).isEqualTo(1250);
            assertThat(accepted.refund().destination()).isEqualTo("bank-account");
        });
    }

    @Test
    void anOrderThatDoesNotExistCannotBeRefunded() {
        assertThat(refunds.refund("no-such-order", toCard(100, "4242"))).isEqualTo(new Outcome.UnknownOrder());
    }
}
