package com.arc_e_tect.gradle.apionly.transcriberj.wiremock;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The emitter's own JSON, read and written without a library, held to the JSON the tests' mapper reads. */
class JsonTest {

    @Test
    void readsEveryKindOfValue() {
        Object value = Json.read(" {\"a\" : [1, -2.5e3, true, false, null], \"b\\\"c\": {\"d\": \"e\\n\\t\\u00e9\\\\/\"},"
                + " \"f\": {}, \"g\": []} ");
        Map<String, Object> expected = new LinkedHashMap<>();
        java.util.ArrayList<Object> a = new java.util.ArrayList<>(List.of(new BigDecimal("1"), new BigDecimal("-2.5e3"),
                true, false));
        a.add(null);
        expected.put("a", a);
        expected.put("b\"c", Map.of("d", "e\n\té\\/"));
        expected.put("f", Map.of());
        expected.put("g", List.of());
        assertThat(value).isEqualTo(expected);
    }

    @Test
    void writesWhatAMapperReadsBackTheSame() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("text", "quote \" backslash \\ newline \n tab \t return \r bell \u0007 backspace \b feed \f é");
        value.put("number", new BigDecimal("12.50"));
        value.put("int", 3);
        value.put("list", List.of("x", false, Map.of()));
        value.put("empty", List.of());
        value.put("nothing", null);
        String written = Json.write(value);
        assertThat(written).endsWith("}\n");
        assertThat(Oracle.JSON.readTree(written)).isEqualTo(Oracle.JSON.readTree(Oracle.JSON.writeValueAsString(value)));
        assertThat(Json.read(written)).isEqualTo(Json.read(Oracle.JSON.writeValueAsString(value)));
    }

    @Test
    void refusesWhatIsNotJson() {
        for (String text : List.of("", "{", "{\"a\" 1}", "{a: 1}", "[1,]", "\"open", "\"\\u12\"", "tru", "1 2", "-")) {
            assertThatThrownBy(() -> Json.read(text)).as(text).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
