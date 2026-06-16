package com.hoaitran.shortlink.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = UrlValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidUrl {
    String message() default "Invalid URL format. Must start with http:// or https:// and contain a valid domain.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
