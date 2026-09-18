package com.example.refunds;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Decides what becomes of a refund request.
 */
@Service
public class Refunds {

    private static final int MINIMUM = 1;
    private static final int MAXIMUM = 1_000_000;
    private static final Pattern CARD_LAST4 = Pattern.compile("^[0-9]{4}$");
    private static final Pattern IBAN = Pattern.compile("^[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}$");

    private final Orders orders;

    /**
     * A service over these orders.
     *
     * @param orders the orders
     */
    public Refunds(Orders orders) {
        this.orders = orders;
    }

    /**
     * Refunds part or all of an order.
     *
     * @param orderId the order
     * @param request the request
     * @return what became of it
     */
    public Outcome refund(String orderId, RefundRequest request) {
        List<FieldError> errors = check(request);
        if (!errors.isEmpty()) {
            return new Outcome.Invalid(errors);
        }
        Orders.Order order = orders.find(orderId).orElse(null);
        if (order == null) {
            return new Outcome.UnknownOrder();
        }
        if (!order.paidWith().equals(request.destination())) {
            return new Outcome.Rejected("The order was paid by " + order.paidWith()
                    + ", so its refunds go back to the " + order.paidWith() + ".");
        }
        if (request.amount() > order.left()) {
            return new Outcome.Rejected("Only " + order.left() + " of the order is left to refund.");
        }
        orders.refund(orderId, request.amount());
        return new Outcome.Accepted(new Refund(UUID.randomUUID().toString(), orderId, request.amount(),
                request.destination()));
    }

    private static List<FieldError> check(RefundRequest request) {
        List<FieldError> errors = new ArrayList<>();
        String destination = request.destination();
        if (!"card".equals(destination) && !"bank-account".equals(destination)) {
            errors.add(new FieldError("destination", "must be card or bank-account"));
            return errors;
        }
        if (request.amount() == null) {
            errors.add(new FieldError("amount", "is required"));
        } else if (request.amount() < MINIMUM || request.amount() > MAXIMUM) {
            errors.add(new FieldError("amount", "must be between " + MINIMUM + " and " + MAXIMUM));
        }
        if (destination.equals("card") && !matches(CARD_LAST4, request.cardLast4())) {
            errors.add(new FieldError("cardLast4", "must be the last four digits of the card"));
        }
        if (destination.equals("bank-account") && !matches(IBAN, request.iban())) {
            errors.add(new FieldError("iban", "must be an IBAN without spaces"));
        }
        return errors;
    }

    private static boolean matches(Pattern pattern, String value) {
        return value != null && pattern.matcher(value).matches();
    }
}
