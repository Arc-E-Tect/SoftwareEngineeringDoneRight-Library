package com.arc_e_tect.fixtures.domainvalidation.execution;

import java.util.function.Supplier;

/** Stands in for the asynchronous execution component that controllers may use. */
public class ValidationExecution {

    public <T> T run(String validation, Supplier<T> validate) {
        return validate.get();
    }
}
