package com.example.refunds;

/**
 * What is wrong with one member of a request.
 *
 * @param field   the member
 * @param message what is wrong with it
 */
public record FieldError(String field, String message) {
}
