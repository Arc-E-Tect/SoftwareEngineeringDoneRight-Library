package com.example.contract;

/**
 * Refund requests, written by hand: {@link RefundRequestV1#body(Object...)} cannot be
 * generated, because the contract's {@code oneOf} branches are inline.
 *
 * <p>It sits in the generated package, in the test source set, so it writes JSON with the
 * same {@link ContractJson} the generated bodies do, and it takes everything else it can
 * from the generated classes. The member names and the two {@code destination} values are
 * the only things typed here; {@link #WRITTEN_AGAINST} is how a change to them is noticed.
 */
public final class RefundRequests {

    /**
     * The {@link RefundRequestV1#FRAGMENT_SHA256} this class was written against. When the
     * request schema changes, a test fails until this class is checked against it again.
     */
    public static final String WRITTEN_AGAINST =
            "f20b627c2a6f8effb19706bfc3bc3b2854a494c0b8bcf79598c46db8666529b6";

    /** An amount the contract accepts: the smallest. */
    public static final int SOME_AMOUNT = AmountV1.MINIMUM;

    /** An amount the contract rejects: one more than the largest. */
    public static final int TOO_LARGE_AN_AMOUNT = AmountV1.MAXIMUM + 1;

    /** Card digits the contract accepts; checked against {@link CardLast4V1#PATTERN}. */
    public static final String SOME_CARD_LAST4 = "4242";

    /** An account the contract accepts; checked against {@link IbanV1#PATTERN}. */
    public static final String SOME_IBAN = "NL91ABNA0417164300";

    private RefundRequests() {
    }

    /**
     * A refund to the card the order was paid with.
     *
     * @param amount    the amount, in cents; see {@link AmountV1}
     * @param cardLast4 the card's last four digits; see {@link CardLast4V1#PATTERN}
     * @return the request body
     */
    public static String toCard(int amount, String cardLast4) {
        StringBuilder json = new StringBuilder("{");
        ContractJson.member(json, "destination", ContractJson.string("card"));
        ContractJson.member(json, "amount", String.valueOf(amount));
        ContractJson.member(json, "cardLast4", ContractJson.string(cardLast4));
        return ContractJson.close(json);
    }

    /**
     * A refund to a bank account.
     *
     * @param amount the amount, in cents; see {@link AmountV1}
     * @param iban   the account; see {@link IbanV1#PATTERN}
     * @return the request body
     */
    public static String toBankAccount(int amount, String iban) {
        StringBuilder json = new StringBuilder("{");
        ContractJson.member(json, "destination", ContractJson.string("bank-account"));
        ContractJson.member(json, "amount", String.valueOf(amount));
        ContractJson.member(json, "iban", ContractJson.string(iban));
        return ContractJson.close(json);
    }
}
