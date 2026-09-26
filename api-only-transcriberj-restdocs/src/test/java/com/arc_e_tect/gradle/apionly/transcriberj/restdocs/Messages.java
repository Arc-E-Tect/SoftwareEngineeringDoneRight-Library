package com.arc_e_tect.gradle.apionly.transcriberj.restdocs;

import tools.jackson.databind.JsonNode;

/** The status messages the README promises, written out from a case as the report records it. */
final class Messages {

    private Messages() {
    }

    /** Where a case's fault is, as a message names it. */
    static String where(JsonNode c) {
        String in = c.get("in").stringValue();
        if (!in.equals("body")) return in + " " + c.get("name").stringValue();
        String pointer = c.get("pointer").stringValue();
        return pointer.isEmpty() ? "the body" : "body " + pointer;
    }

    /** What every status message starts with. */
    static String subject(JsonNode c, int received) {
        return "Case " + c.get("id").stringValue() + " (" + c.get("description").stringValue() + "; "
                + c.get("keyword").stringValue() + " at " + where(c) + "): expected "
                + c.get("expectedStatus").asInt() + ", received " + received + ".";
    }

    /** The message for a request the implementation accepted. */
    static String accepted(JsonNode c, int received) {
        return subject(c, received) + " The implementation accepted a request the contract declares invalid: `"
                + c.get("keyword").stringValue() + "` at " + where(c) + " is not enforced.";
    }

    /** The message for a request answered 404. */
    static String notFound(JsonNode c) {
        return subject(c, 404) + " The implementation looked the resource up before validating the request. "
                + "The inbound adapter should reject contract violations before the port is called.";
    }

    /** The message for a 400 answered 422. */
    static String domain(JsonNode c) {
        return subject(c, 422) + " A contract violation was treated as a domain violation. It should be rejected "
                + "by the adapter, not the domain.";
    }

    /** The message for a request the implementation failed on. */
    static String failed(JsonNode c, int received) {
        return subject(c, received) + " The implementation failed instead of rejecting the request.";
    }
}
