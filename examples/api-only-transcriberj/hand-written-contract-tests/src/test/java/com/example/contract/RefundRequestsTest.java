package com.example.contract;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * What keeps the hand-written {@link RefundRequests} honest as the contract moves.
 */
class RefundRequestsTest {

    @Test
    void theHandWrittenBodyWasWrittenAgainstTheRequestSchemaAsItIsNow() {
        assertThat(RefundRequestV1.FRAGMENT_SHA256)
                .as("%s changed; check RefundRequests against it, then update WRITTEN_AGAINST",
                        RefundRequestV1.FRAGMENT_PATH)
                .isEqualTo(RefundRequests.WRITTEN_AGAINST);
    }

    @Test
    void theGeneratorStillCannotWriteThisBody() {
        // When this fails, the contract names its branches and the generator writes the
        // body itself: use the generated classes, and delete RefundRequests.
        assertThatThrownBy(RefundRequestV1::body)
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("ONE_OF_INLINE_BRANCHES");
    }

    @Test
    void theSampleValuesAreOnesTheContractAccepts() {
        // CardLast4V1 and IbanV1 are package-private: nothing public in the generated tree
        // takes them, because the body that would have is the one that could not be generated.
        assertThat(RefundRequests.SOME_AMOUNT).isBetween(AmountV1.MINIMUM, AmountV1.MAXIMUM);
        assertThat(RefundRequests.SOME_CARD_LAST4).matches(CardLast4V1.PATTERN);
        assertThat(RefundRequests.SOME_IBAN).matches(IbanV1.PATTERN);
    }

    @Test
    void itWritesTheMembersEachBranchRequires() {
        assertThat(RefundRequests.toCard(RefundRequests.SOME_AMOUNT, RefundRequests.SOME_CARD_LAST4))
                .isEqualTo("{\"destination\":\"card\",\"amount\":1,\"cardLast4\":\"4242\"}\n");
        assertThat(RefundRequests.toBankAccount(RefundRequests.SOME_AMOUNT, RefundRequests.SOME_IBAN))
                .isEqualTo("{\"destination\":\"bank-account\",\"amount\":1,\"iban\":\"NL91ABNA0417164300\"}\n");
    }
}
