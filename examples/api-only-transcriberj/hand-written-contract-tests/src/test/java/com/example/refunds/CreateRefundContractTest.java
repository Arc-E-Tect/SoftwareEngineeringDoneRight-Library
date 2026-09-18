package com.example.refunds;

import com.example.contract.CreateRefundOperation;
import com.example.contract.RefundRequests;
import com.example.contract.restdocs.ProblemV1Docs;
import com.example.contract.restdocs.RefundV1Docs;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The contract tests of {@code createRefund}: one per response the contract lists, each
 * checking the response's shape -- its status, its content type and its fields -- against
 * the contract. Which requests the service rejects, and why, is behaviour, and is tested
 * in {@link RefundsTest}.
 */
// The service itself, not a stand-in: the answers are the ones a client gets.
@WebMvcTest(RefundController.class)
@Import({Refunds.class, Orders.class})
@AutoConfigureRestDocs
class CreateRefundContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void anAcceptedRefundIsAnsweredInTheShapeTheContractDescribes() throws Exception {
        mockMvc.perform(post(CreateRefundOperation.PATH, Orders.PAID_BY_CARD)
                        .contentType(CreateRefundOperation.REQUEST_CONTENT_TYPE)
                        .content(RefundRequests.toCard(RefundRequests.SOME_AMOUNT, RefundRequests.SOME_CARD_LAST4)))
                .andExpect(status().is(CreateRefundOperation.STATUS_201))
                .andExpect(content().contentType(CreateRefundOperation.CONTENT_TYPE_201))
                .andDo(document("create-refund", RefundV1Docs.responseFields()));
    }

    @Test
    void aRequestTheContractRejectsIsAnsweredWithAProblem() throws Exception {
        // One example of a request the contract does not allow is enough to see the shape
        // of the answer. Every other way to be invalid is behaviour: see RefundsTest.
        mockMvc.perform(post(CreateRefundOperation.PATH, Orders.PAID_BY_CARD)
                        .contentType(CreateRefundOperation.REQUEST_CONTENT_TYPE)
                        .content(RefundRequests.toCard(RefundRequests.TOO_LARGE_AN_AMOUNT, RefundRequests.SOME_CARD_LAST4)))
                .andExpect(status().is(CreateRefundOperation.STATUS_400))
                .andExpect(content().contentType(CreateRefundOperation.CONTENT_TYPE_400))
                .andDo(document("create-refund-invalid", ProblemV1Docs.responseFields()));
    }

    @Test
    void anUnknownOrderIsAnsweredWithAProblem() throws Exception {
        mockMvc.perform(post(CreateRefundOperation.PATH, "no-such-order")
                        .contentType(CreateRefundOperation.REQUEST_CONTENT_TYPE)
                        .content(RefundRequests.toCard(RefundRequests.SOME_AMOUNT, RefundRequests.SOME_CARD_LAST4)))
                .andExpect(status().is(CreateRefundOperation.STATUS_404))
                .andExpect(content().contentType(CreateRefundOperation.CONTENT_TYPE_404))
                .andDo(document("create-refund-unknown-order", ProblemV1Docs.responseFields()));
    }

    @Test
    void aRefundThatBreaksABusinessRuleIsAnsweredWithAProblem() throws Exception {
        // Valid against the schema, and still refused: the order was paid by card, so its
        // refund goes back to the card. The schema cannot say that; the 422 is how the
        // contract tells a client, and its shape is what this test checks.
        mockMvc.perform(post(CreateRefundOperation.PATH, Orders.PAID_BY_CARD)
                        .contentType(CreateRefundOperation.REQUEST_CONTENT_TYPE)
                        .content(RefundRequests.toBankAccount(RefundRequests.SOME_AMOUNT, RefundRequests.SOME_IBAN)))
                .andExpect(status().is(CreateRefundOperation.STATUS_422))
                .andExpect(content().contentType(CreateRefundOperation.CONTENT_TYPE_422))
                .andDo(document("create-refund-rejected", ProblemV1Docs.responseFields()));
    }
}
