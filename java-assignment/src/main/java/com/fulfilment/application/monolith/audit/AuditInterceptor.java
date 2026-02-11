package com.fulfilment.application.monolith.audit;

import com.fulfilment.application.monolith.stores.Store;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import java.lang.reflect.Method;
import org.jboss.logging.Logger;

/**
 * CDI Interceptor that automatically audits methods on @Audited classes.
 *
 * Derives the action type from JAX-RS annotations and extracts
 * resource identifiers from method parameters. Delegates to
 * AuditService (REQUIRES_NEW) so audit records survive rollbacks.
 *
 * @see Audited
 * @see AuditService
 */
@Audited
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class AuditInterceptor {

  private static final Logger LOGGER = Logger.getLogger(AuditInterceptor.class);

  @Inject AuditService auditService;

  @AroundInvoke
  public Object auditMethodCall(InvocationContext ctx) throws Exception {
    Method method = ctx.getMethod();
    String action = resolveAction(method);

    if (action == null) {
      return ctx.proceed();
    }

    String resourceIdentifier = resolveResourceIdentifier(ctx);

    LOGGER.debugf("Audit interceptor: action=%s, resource=%s, method=%s",
        action, resourceIdentifier, method.getName());

    auditService.logAction(resourceIdentifier, action, "ATTEMPTED");

    return ctx.proceed();
  }

  private String resolveAction(Method method) {
    if (method.isAnnotationPresent(POST.class)) return "CREATED";
    if (method.isAnnotationPresent(PUT.class)) return "UPDATED";
    if (method.isAnnotationPresent(PATCH.class)) return "PATCHED";
    if (method.isAnnotationPresent(DELETE.class)) return "DELETED";
    return null;
  }

  private String resolveResourceIdentifier(InvocationContext ctx) {
    String className = resolveTargetClassName(ctx);
    Object[] params = ctx.getParameters();

    for (Object param : params) {
      if (param instanceof Store store && store.name != null) {
        return store.name;
      }
    }

    for (Object param : params) {
      if (param instanceof Long id) {
        return className + "(id=" + id + ")";
      }
    }

    return className;
  }

  private String resolveTargetClassName(InvocationContext ctx) {
    Class<?> targetClass = ctx.getTarget().getClass();
    if (targetClass.getName().contains("_Subclass") || targetClass.getName().contains("$$")) {
      targetClass = targetClass.getSuperclass();
    }
    return targetClass.getSimpleName();
  }
}
