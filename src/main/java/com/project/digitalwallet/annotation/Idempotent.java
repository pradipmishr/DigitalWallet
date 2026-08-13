package com.project.digitalwallet.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {
    /** Expiration window in seconds for the completed response (default: 24 hours) */
    long expireInSeconds() default 86400;

    /** Header name used to supply the idempotency key */
    String headerName() default "Idempotency-Key";
}