package com.arc_e_tect.gradle.apionly.transcriberj.wiremock;

import tools.jackson.databind.JsonNode;

/**
 * The status messages the REST Docs emitter's README promises, written out from a case as the
 * report records it. Copied from that emitter's tests.
 */
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
}
