package com.example.refunds;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code POST /orders/{orderId}/refunds}.
 */
@RestController
public class RefundController {

    private final Refunds refunds;

    /**
     * A controller over this service.
     *
     * @param refunds the service
     */
    public RefundController(Refunds refunds) {
        this.refunds = refunds;
    }

    /**
     * Refunds part or all of an order.
     *
     * @param orderId the order
     * @param request the request
     * @return the refund, or the problem
     */
    @PostMapping(path = "/orders/{orderId}/refunds", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> refund(@PathVariable String orderId, @RequestBody RefundRequest request) {
        return switch (refunds.refund(orderId, request)) {
            case Outcome.Accepted accepted -> ResponseEntity.status(HttpStatus.CREATED)
                    .contentType(MediaType.APPLICATION_JSON).body(accepted.refund());
            case Outcome.Invalid invalid -> problem(HttpStatus.BAD_REQUEST,
                    "The request does not match the contract.", null, invalid.errors());
            case Outcome.UnknownOrder unknown -> problem(HttpStatus.NOT_FOUND,
                    "No order has that identifier.", "There is no order " + orderId + ".", null);
            case Outcome.Rejected rejected -> problem(HttpStatus.UNPROCESSABLE_CONTENT,
                    "The refund breaks a business rule.", rejected.detail(), null);
        };
    }

    private static ResponseEntity<Problem> problem(HttpStatus status, String title, String detail,
                                                   java.util.List<FieldError> errors) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(new Problem(title, status.value(), detail, errors));
    }
}
