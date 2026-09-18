package com.example.refunds;

import java.util.List;

/**
 * What became of a refund request.
 */
public sealed interface Outcome {

    /**
     * The refund was accepted.
     *
     * @param refund the refund
     */
    record Accepted(Refund refund) implements Outcome {
    }

    /**
     * The request is not one the contract allows.
     *
     * @param errors what is wrong with it, member by member
     */
    record Invalid(List<FieldError> errors) implements Outcome {
    }

    /** No order has the identifier the request names. */
    record UnknownOrder() implements Outcome {
    }

    /**
     * The request is one the contract allows, and it breaks a business rule.
     *
     * @param detail which rule, and how
     */
    record Rejected(String detail) implements Outcome {
    }
}
