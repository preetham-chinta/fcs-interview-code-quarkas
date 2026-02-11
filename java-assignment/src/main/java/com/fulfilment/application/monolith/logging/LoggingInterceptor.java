package com.fulfilment.application.monolith.logging;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;

/**
 * Interceptor that provides structured method-level logging.
 *
 * Automatically logs:
 *   - Method entry with parameter summary (truncated for safety)
 *   - Method exit with execution time in milliseconds
 *   - Exceptions with execution time and exception type/message
 *
 * Priority is set BEFORE {@code Interceptor.Priority.APPLICATION} so logging
 * wraps around business interceptors like @Audited. This means:
 *   LoggingInterceptor.enter → AuditInterceptor.enter → method → AuditInterceptor.exit → LoggingInterceptor.exit
 *
 * The interceptor resolves the real class name (unwrapping CDI proxies) and
 * uses the target class's logger category for easy filtering (e.g.,
 * {@code quarkus.log.category."com.fulfilment.application.monolith.stores.StoreService".level=DEBUG}).
 *
 * @see Logged
 */
@Logged
@Interceptor
@Priority(Interceptor.Priority.APPLICATION - 10)
public class LoggingInterceptor {

  /** Max characters for a single parameter's toString() representation. */
  private static final int PARAM_TRUNCATE_LENGTH = 100;

  @AroundInvoke
  public Object logMethodCall(InvocationContext ctx) throws Exception {
    Class<?> targetClass = resolveTargetClass(ctx);
    Logger logger = Logger.getLogger(targetClass);
    Method method = ctx.getMethod();
    String methodName = method.getName();

    String paramSummary = summarizeParameters(ctx.getParameters());

    logger.infof("→ %s(%s)", methodName, paramSummary);

    long startNanos = System.nanoTime();
    try {
      Object result = ctx.proceed();
      long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;

      logger.infof("← %s completed in %d ms", methodName, elapsedMs);

      return result;
    } catch (Exception ex) {
      long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;

      logger.errorf("✗ %s failed in %d ms — %s: %s",
          methodName, elapsedMs,
          ex.getClass().getSimpleName(), ex.getMessage());

      throw ex;
    }
  }

  /**
   * Resolves the real target class, unwrapping CDI proxy subclasses.
   *
   * Quarkus generates proxy classes with names like {@code StoreService_Subclass}
   * or {@code StoreService$$EnhancerByQuarkus}. We walk up to the real class
   * so log messages use the developer-recognisable name.
   */
  Class<?> resolveTargetClass(InvocationContext ctx) {
    Class<?> targetClass = ctx.getTarget().getClass();
    while (targetClass.getName().contains("_Subclass")
        || targetClass.getName().contains("$$")
        || targetClass.getName().contains("_ClientProxy")) {
      targetClass = targetClass.getSuperclass();
    }
    return targetClass;
  }

  /**
   * Creates a human-readable summary of method parameters.
   *
   * Each parameter's toString() is truncated to {@link #PARAM_TRUNCATE_LENGTH}
   * characters to prevent log flooding from large objects. Null values are
   * rendered as "null".
   */
  String summarizeParameters(Object[] params) {
    if (params == null || params.length == 0) {
      return "";
    }
    return Arrays.stream(params)
        .map(this::truncate)
        .collect(Collectors.joining(", "));
  }

  private String truncate(Object param) {
    if (param == null) {
      return "null";
    }
    String value = param.toString();
    if (value.length() > PARAM_TRUNCATE_LENGTH) {
      return value.substring(0, PARAM_TRUNCATE_LENGTH) + "…";
    }
    return value;
  }
}
