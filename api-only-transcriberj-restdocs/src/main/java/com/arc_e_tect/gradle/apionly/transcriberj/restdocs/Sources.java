package com.arc_e_tect.gradle.apionly.transcriberj.restdocs;

/** The Java sources the emitter writes. */
final class Sources {

    private Sources() {
    }

    /** The one class that turns the core's field descriptions into REST Docs descriptors. */
    static String fields(String header, String pkg, String base) {
        return header + """
                package %1$s;

                import %2$s.ContractField;
                import org.springframework.restdocs.payload.FieldDescriptor;
                import org.springframework.restdocs.payload.JsonFieldType;
                import org.springframework.restdocs.payload.PayloadDocumentation;

                import java.util.List;

                /**
                 * Spring REST Docs descriptors from the field descriptions in {@code %2$s}.
                 */
                public final class RestDocsFields {

                    private RestDocsFields() {
                    }

                    /**
                     * One descriptor per field, in order.
                     *
                     * @param fields the fields, as a core class describes them
                     * @return the descriptors
                     */
                    public static FieldDescriptor[] of(List<ContractField> fields) {
                        return fields.stream().map(RestDocsFields::of).toArray(FieldDescriptor[]::new);
                    }

                    /**
                     * The descriptor of one field: a subsection where the field stands for
                     * everything beneath it.
                     *
                     * @param field the field
                     * @return the descriptor
                     */
                    public static FieldDescriptor of(ContractField field) {
                        FieldDescriptor descriptor = field.subsection()
                                ? PayloadDocumentation.subsectionWithPath(field.path())
                                : PayloadDocumentation.fieldWithPath(field.path());
                        descriptor.description(field.description()).type(type(field.type()));
                        return field.optional() ? descriptor.optional() : descriptor;
                    }

                    private static JsonFieldType type(String type) {
                        return switch (type) {
                            case "string" -> JsonFieldType.STRING;
                            case "number" -> JsonFieldType.NUMBER;
                            case "boolean" -> JsonFieldType.BOOLEAN;
                            case "object" -> JsonFieldType.OBJECT;
                            case "array" -> JsonFieldType.ARRAY;
                            case "null" -> JsonFieldType.NULL;
                            default -> JsonFieldType.VARIES;
                        };
                    }
                }
                """.formatted(pkg, base);
    }

    /** The companion of one core class. */
    static String companion(String header, String pkg, String base, String name, String openClass) {
        return header + """
                package %1$s;

                import %2$s.%3$s;
                import org.springframework.restdocs.payload.FieldDescriptor;
                import org.springframework.restdocs.payload.PayloadDocumentation;
                import org.springframework.restdocs.payload.RequestFieldsSnippet;
                import org.springframework.restdocs.payload.ResponseFieldsSnippet;

                /**
                 * Spring REST Docs descriptors and snippets of {@link %3$s}.
                 */
                public final class %3$sDocs {

                    private %3$sDocs() {
                    }

                    /**
                     * The descriptors of a body's fields.
                     *
                     * @return the descriptors
                     */
                    public static FieldDescriptor[] fields() {
                        return fields("");
                    }

                    /**
                     * The descriptors of a body's fields, each path starting with {@code prefix}.
                     *
                     * @param prefix what every path starts with, such as {@code user.}
                     * @return the descriptors
                     */
                    public static FieldDescriptor[] fields(String prefix) {
                        return RestDocsFields.of(%3$s.fields(prefix));
                    }

                    /**
                     * The snippet documenting a request with this body: relaxed when the
                     * contract allows members it does not list.
                     *
                     * @return the snippet
                     */
                    public static RequestFieldsSnippet requestFields() {
                        return %2$s.%4$s.OPEN
                                ? PayloadDocumentation.relaxedRequestFields(fields())
                                : PayloadDocumentation.requestFields(fields());
                    }

                    /**
                     * The snippet documenting a response with this body: relaxed when the
                     * contract allows members it does not list.
                     *
                     * @return the snippet
                     */
                    public static ResponseFieldsSnippet responseFields() {
                        return %2$s.%4$s.OPEN
                                ? PayloadDocumentation.relaxedResponseFields(fields())
                                : PayloadDocumentation.responseFields(fields());
                    }
                }
                """.formatted(pkg, base, name, openClass);
    }
}
