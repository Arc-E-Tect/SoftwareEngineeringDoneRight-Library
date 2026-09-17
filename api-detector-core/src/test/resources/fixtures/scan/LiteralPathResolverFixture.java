package com.example.fixture.scan;

public interface LiteralPathResolverFixture {

    String USER_BY_USERNAME_PATH = "/v1/users/{username}";

    String COMPLETION_PATH = ApiEndpoints.get("users.registrations.completion");

    String ROOT_PATH = GetRootOperation.PATH;

    Object client();

    default void literalArgument() {
        client().get("/v1/health");
    }

    default void fieldConstantArgument() {
        client().get(USER_BY_USERNAME_PATH);
    }

    default void localVariableArgument() {
        String path = "/v1/users/registrations";
        client().post(path);
    }

    default void methodCallArgument() {
        client().delete(dynamicPath());
    }

    default void helperMethodArgument() {
        client().post(ApiEndpoints.get("users.registrations"));
    }

    default void helperMethodUnknownKeyArgument() {
        client().post(ApiEndpoints.get("users.unknown"));
    }

    default void helperMethodFieldConstantArgument() {
        client().get(COMPLETION_PATH);
    }

    default void qualifiedConstantArgument() {
        client().get(GetUserOperation.PATH);
    }

    default void fullyQualifiedConstantArgument() {
        client().get(com.example.contract.GetUserOperation.PATH);
    }

    default void qualifiedConstantUnknownArgument() {
        client().get(GetUserOperation.UNKNOWN);
    }

    default void computedScopeConstantArgument() {
        client().get(operation().PATH);
    }

    default void qualifiedConstantFieldArgument() {
        client().get(ROOT_PATH);
    }

    default void valueAnnotationArgument() {
        client().get(valueAnnotatedPath);
    }

    default void valueAnnotationWithDefaultArgument() {
        client().get(valueAnnotatedPathWithDefault);
    }

    default void valueAnnotationUnknownKeyNoDefaultArgument() {
        client().get(valueAnnotatedPathUnknownKeyNoDefault);
    }

    @Value("${users.by-username}")
    String valueAnnotatedPath = null;

    @Value("${users.unknown:/v1/fallback}")
    String valueAnnotatedPathWithDefault = null;

    @Value("${users.unknown}")
    String valueAnnotatedPathUnknownKeyNoDefault = null;

    private static String dynamicPath() {
        return "/v1/dynamic";
    }

    final class ApiEndpoints {
        private ApiEndpoints() {}

        static String get(String key) {
            return switch (key) {
                case "users.registrations" -> "/v1/users/registrations";
                case "users.registrations.completion" -> "/v1/users/registrations/completion/{verificationLink}";
                default -> throw new IllegalArgumentException("Unknown key: " + key);
            };
        }
    }

    @interface Value {
        String value();
    }
}
