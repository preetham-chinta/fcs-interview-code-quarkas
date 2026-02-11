package com.fulfilment.application.monolith.logging;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.interceptor.InvocationContext;
import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link LoggingInterceptor}.
 *
 * Tests cover:
 *   - Successful method invocation (proceed is called and result returned)
 *   - Exception propagation (exceptions re-thrown after logging)
 *   - Parameter summarization (null, empty, truncation)
 *   - CDI proxy class resolution (unwrapping _Subclass, $$, _ClientProxy)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LoggingInterceptor")
class LoggingInterceptorTest {

  @InjectMocks private LoggingInterceptor interceptor;

  @Mock private InvocationContext ctx;

  // ── A dummy class for resolving target class names ──────────────────
  static class FakeService {}

  static class FakeService_Subclass extends FakeService {}

  static class FakeService$$EnhancerByQuarkus extends FakeService {}

  static class FakeService_ClientProxy extends FakeService {}

  // ──────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("logMethodCall — happy path")
  class HappyPath {

    @Test
    @DisplayName("should invoke ctx.proceed() and return its result")
    void shouldProceedAndReturnResult() throws Exception {
      FakeService target = new FakeService();
      Method method = FakeService.class.getMethod("toString");

      when(ctx.getTarget()).thenReturn(target);
      when(ctx.getMethod()).thenReturn(method);
      when(ctx.getParameters()).thenReturn(new Object[]{});
      when(ctx.proceed()).thenReturn("ok");

      Object result = interceptor.logMethodCall(ctx);

      assertEquals("ok", result);
      verify(ctx).proceed();
    }

    @Test
    @DisplayName("should invoke ctx.proceed() even when result is null")
    void shouldHandleNullReturnValue() throws Exception {
      FakeService target = new FakeService();
      Method method = FakeService.class.getMethod("toString");

      when(ctx.getTarget()).thenReturn(target);
      when(ctx.getMethod()).thenReturn(method);
      when(ctx.getParameters()).thenReturn(new Object[]{});
      when(ctx.proceed()).thenReturn(null);

      Object result = interceptor.logMethodCall(ctx);

      assertNull(result);
      verify(ctx).proceed();
    }
  }

  @Nested
  @DisplayName("logMethodCall — exception handling")
  class ExceptionHandling {

    @Test
    @DisplayName("should re-throw exception from ctx.proceed()")
    void shouldRethrowException() throws Exception {
      FakeService target = new FakeService();
      Method method = FakeService.class.getMethod("toString");

      when(ctx.getTarget()).thenReturn(target);
      when(ctx.getMethod()).thenReturn(method);
      when(ctx.getParameters()).thenReturn(new Object[]{});
      when(ctx.proceed()).thenThrow(new IllegalStateException("boom"));

      IllegalStateException thrown =
          assertThrows(IllegalStateException.class, () -> interceptor.logMethodCall(ctx));

      assertEquals("boom", thrown.getMessage());
    }

    @Test
    @DisplayName("should re-throw RuntimeException unchanged")
    void shouldRethrowRuntimeException() throws Exception {
      FakeService target = new FakeService();
      Method method = FakeService.class.getMethod("toString");

      when(ctx.getTarget()).thenReturn(target);
      when(ctx.getMethod()).thenReturn(method);
      when(ctx.getParameters()).thenReturn(new Object[]{});
      when(ctx.proceed()).thenThrow(new NullPointerException("npe"));

      NullPointerException thrown =
          assertThrows(NullPointerException.class, () -> interceptor.logMethodCall(ctx));

      assertEquals("npe", thrown.getMessage());
    }
  }

  @Nested
  @DisplayName("summarizeParameters")
  class SummarizeParameters {

    @Test
    @DisplayName("should return empty string for no parameters")
    void shouldReturnEmptyForNoParams() {
      assertEquals("", interceptor.summarizeParameters(new Object[]{}));
    }

    @Test
    @DisplayName("should return empty string for null array")
    void shouldReturnEmptyForNullArray() {
      assertEquals("", interceptor.summarizeParameters(null));
    }

    @Test
    @DisplayName("should render null parameters as 'null'")
    void shouldRenderNullAsString() {
      String result = interceptor.summarizeParameters(new Object[]{null, "hello"});
      assertEquals("null, hello", result);
    }

    @Test
    @DisplayName("should join multiple parameters with comma")
    void shouldJoinWithComma() {
      String result = interceptor.summarizeParameters(new Object[]{"a", 42, true});
      assertEquals("a, 42, true", result);
    }

    @Test
    @DisplayName("should truncate parameters longer than 100 characters")
    void shouldTruncateLongParameters() {
      String longString = "x".repeat(150);
      String result = interceptor.summarizeParameters(new Object[]{longString});

      assertTrue(result.length() < 150, "Should be shorter than original");
      assertTrue(result.endsWith("…"), "Should end with ellipsis");
      assertEquals(101, result.length()); // 100 chars + ellipsis
    }
  }

  @Nested
  @DisplayName("resolveTargetClass — CDI proxy unwrapping")
  class ResolveTargetClass {

    @Test
    @DisplayName("should return the class itself when not a proxy")
    void shouldReturnRealClass() {
      FakeService target = new FakeService();
      when(ctx.getTarget()).thenReturn(target);

      Class<?> resolved = interceptor.resolveTargetClass(ctx);

      assertEquals(FakeService.class, resolved);
    }

    @Test
    @DisplayName("should unwrap _Subclass proxy to real class")
    void shouldUnwrapSubclassProxy() {
      FakeService_Subclass proxy = new FakeService_Subclass();
      when(ctx.getTarget()).thenReturn(proxy);

      Class<?> resolved = interceptor.resolveTargetClass(ctx);

      assertEquals(FakeService.class, resolved);
    }

    @Test
    @DisplayName("should unwrap $$EnhancerByQuarkus proxy to real class")
    void shouldUnwrapEnhancerProxy() {
      FakeService$$EnhancerByQuarkus proxy = new FakeService$$EnhancerByQuarkus();
      when(ctx.getTarget()).thenReturn(proxy);

      Class<?> resolved = interceptor.resolveTargetClass(ctx);

      assertEquals(FakeService.class, resolved);
    }

    @Test
    @DisplayName("should unwrap _ClientProxy to real class")
    void shouldUnwrapClientProxy() {
      FakeService_ClientProxy proxy = new FakeService_ClientProxy();
      when(ctx.getTarget()).thenReturn(proxy);

      Class<?> resolved = interceptor.resolveTargetClass(ctx);

      assertEquals(FakeService.class, resolved);
    }
  }
}
