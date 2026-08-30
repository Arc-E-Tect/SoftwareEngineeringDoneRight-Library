package com.example.fixture.scan;

public interface LiteralPathResolverFixture {

    String USER_BY_USERNAME_PATH = "/v1/users/{username}";

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

    private static String dynamicPath() {
        return "/v1/dynamic";
    }
}
