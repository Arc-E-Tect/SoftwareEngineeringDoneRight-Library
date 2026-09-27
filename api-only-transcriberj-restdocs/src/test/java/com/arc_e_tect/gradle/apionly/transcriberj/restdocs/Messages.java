package com.arc_e_tect.gradle.apionly.transcriberj.restdocs;

import tools.jackson.databind.JsonNode;

/** The status messages the README promises, written out from a case as the report records it. */
final class Messages {

    /** What every fixture-related message ends with. */
    static final String CHECK_FIXTURE = " Check arrangeState for this case.";

    private Messages() {
    }

    /** Where an invalid request's fault is, as a message names it. */
    static String where(JsonNode c) {
        String in = c.get("in").stringValue();
        if (!in.equals("body")) return in + " " + c.get("name").stringValue();
        String pointer = c.get("pointer").stringValue();
        return pointer.isEmpty() ? "the body" : "body " + pointer;
    }

    /** How every message names a case. */
    static String name(JsonNode c) {
        String kind = c.get("kind").stringValue();
        String at = kind.equals("INVALID_REQUEST") ? ", at " + where(c) + " " + c.get("keyword").stringValue() : "";
        return c.get("id").stringValue() + " (" + kind + ": " + c.get("description").stringValue() + at + ")";
    }

    /** What every status message starts with. */
    static String subject(JsonNode c, int received) {
        return name(c) + ": expected " + c.get("expectedStatus").asInt() + ", received " + received + ".";
    }

    /** The message for an invalid request the implementation accepted. */
    static String accepted(JsonNode c, int received) {
        return subject(c, received) + " The implementation accepted a request the contract declares invalid: `"
                + c.get("keyword").stringValue() + "` at " + where(c) + " is not enforced.";
    }

    /** The message for an invalid request answered 404. */
    static String notFound(JsonNode c) {
        return subject(c, 404) + " The implementation looked the resource up before validating the request. "
                + "The inbound adapter should reject contract violations before the port is called.";
    }

    /** The message for a 400 answered 422. */
    static String domain(JsonNode c) {
        return subject(c, 422) + " A contract violation was treated as a domain violation. It should be rejected "
                + "by the adapter, not the domain.";
    }

    /** The message for an invalid request the implementation failed on. */
    static String failed(JsonNode c, int received) {
        return subject(c, received) + " The implementation failed instead of rejecting the request.";
    }

    /** The message for a valid request the implementation rejected with 400. */
    static String successRejected(JsonNode c) {
        return subject(c, 400) + " The implementation rejects a request the contract declares valid. Compare its "
                + "validation with the contract: an anchored or stricter pattern, a stricter length or bound, a member "
                + "it requires that the contract makes optional, or an optional member of the full request it does not "
                + "know.";
    }

    /** The message for a success case answered 404. */
    static String successNotFound(JsonNode c) {
        return subject(c, 404) + " The fixture did not create the resource, or the implementation looks for it "
                + "somewhere else." + CHECK_FIXTURE;
    }

    /** The message for a success case answered 409. */
    static String successConflict(JsonNode c) {
        return subject(c, 409) + " State left behind: the fixture did not remove what this request creates."
                + CHECK_FIXTURE;
    }

    /** The message for a success case answered with another 2xx. */
    static String successOtherStatus(JsonNode c, int received) {
        return subject(c, received) + " The implementation answers with a status the contract does not declare for "
                + "this case, or the fixture set up the state for another declared status." + CHECK_FIXTURE;
    }

    /** The message for a success case the implementation failed on. */
    static String successFailed(JsonNode c, int received) {
        return subject(c, received) + " The implementation failed on a request the contract declares valid.";
    }

    /** The message for a not-found case answered with a 2xx. */
    static String notFoundFound(JsonNode c, int received) {
        return subject(c, received) + " The fixture did not remove the resource, or the implementation answers for "
                + "any identifier." + CHECK_FIXTURE;
    }

    /** The message for a not-found case answered 400. */
    static String notFoundRejected(JsonNode c) {
        return subject(c, 400) + " The implementation rejects an identifier the contract declares valid.";
    }

    /** The message for a not-acceptable case answered with a 2xx. */
    static String acceptIgnored(JsonNode c, int received) {
        return subject(c, received) + " Content negotiation is not enforced: the implementation ignores Accept.";
    }

    /** The message for an unsupported-media-type case answered with a 2xx. */
    static String contentTypeAccepted(JsonNode c, int received) {
        return subject(c, received) + " The implementation accepts a Content-Type the contract does not declare for "
                + "this request.";
    }

    /** The message for an unsupported-media-type case answered 400. */
    static String parsedFirst(JsonNode c) {
        return subject(c, 400) + " The implementation parsed the body before checking its media type.";
    }
}
