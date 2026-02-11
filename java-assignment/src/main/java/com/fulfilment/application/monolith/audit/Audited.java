package com.fulfilment.application.monolith.audit;

import jakarta.interceptor.InterceptorBinding;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * CDI InterceptorBinding that enables automatic audit logging.
 *
 * Place on a class to audit all methods, or on individual methods.
 * Mutating operations (POST/PUT/PATCH/DELETE) are audited automatically;
 * GET methods are skipped by the interceptor.
 *
 * @see AuditInterceptor
 */
@Inherited
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Audited {
}
