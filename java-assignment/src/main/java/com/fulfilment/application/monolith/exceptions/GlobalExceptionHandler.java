package com.fulfilment.application.monolith.exceptions;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;

// =============================================================================
// Global Exception Handler — single @Provider that catches ALL exceptions
// =============================================================================
//
// HOW IT WORKS:
//   @Provider tells JAX-RS: "register this globally for every endpoint."
//   ExceptionMapper<Exception> catches the base Exception type, which means
//   ANY unhandled exception from ANY resource ends up here.
//
//   Spring equivalent: @ControllerAdvice + @ExceptionHandler(Exception.class)
//
// WHY A SINGLE CLASS:
//   JAX-RS supports multiple ExceptionMapper<T> with specific types, but
//   a single handler with a resolution chain is simpler to maintain:
//     - One place to see all error handling logic
//     - No risk of missing an exception type (catch-all at the bottom)
//     - Easy to add new types: just add a case in resolveStatusCode/resolveMessage
//
// RESOLUTION CHAIN (most specific → least specific):
//   1. ConstraintViolationException → 400  (Bean Validation: @NotBlank, @Size, etc.)
//   2. BusinessException            → ???  (our domain exceptions: NotFoundException=404, etc.)
//   3. WebApplicationException      → ???  (JAX-RS standard: thrown with explicit status code)
//   4. Everything else              → 500  (unexpected errors, NPE, etc.)
//
//   The chain matters: ConstraintViolationException is checked FIRST because
//   it needs special message handling (joining multiple violations).
//   BusinessException is checked BEFORE WebApplicationException because
//   BusinessException carries domain-specific status codes.
//   WebApplicationException is the JAX-RS built-in — it already has a status.
//   Anything else is an unexpected server error → 500.
//
// RESPONSE FORMAT:
//   Every error returns the same ErrorResponse shape:
//   {
//     "errorId": "a3f2b1c8",           ← unique ID for log correlation
//     "code": 404,                      ← HTTP status code
//     "error": "Store not found",       ← human-readable message
//     "exceptionType": "NotFoundException", ← exception class (for debugging)
//     "timestamp": "2024-07-15T10:30"   ← when the error occurred
//   }
//
// LOGGING STRATEGY:
//   - 5xx errors → LOGGER.error (with full stack trace — these are bugs)
//   - 4xx errors → LOGGER.warn  (no stack trace — these are client mistakes)
//
@Provider
public class GlobalExceptionHandler implements ExceptionMapper<Exception> {

  private static final Logger LOGGER = Logger.getLogger(GlobalExceptionHandler.class);

  @Override
  public Response toResponse(Exception exception) {
    int code = resolveStatusCode(exception);
    String message = resolveMessage(exception);

    // 5xx = server bug → log full stack trace for debugging
    // 4xx = client error → warn only, no stack trace (not our bug)
    if (code >= 500) {
      LOGGER.error("Failed to handle request", exception);
    } else {
      LOGGER.warnf("Request error [%d]: %s", code, message);
    }

    // Build standardized response — same shape for every error type.
    // Jackson auto-serializes ErrorResponse to JSON via quarkus-rest-jackson.
    ErrorResponse error = new ErrorResponse(code, message, exception.getClass().getName());

    return Response.status(code).entity(error).build();
  }

  // --- Resolution chain: exception type → HTTP status code ---
  //
  // Order matters! Most specific first, catch-all (500) last.
  // To add a new exception type, insert a new line ABOVE the return 500.
  //
  // Example — adding a RateLimitException:
  //   if (ex instanceof RateLimitException) return 429;
  //
  private int resolveStatusCode(Exception ex) {
    // @Valid failures: @NotBlank, @Size, @PositiveOrZero violations
    if (ex instanceof ConstraintViolationException) return 400;

    // Our domain exceptions: NotFoundException(404), ConflictException(409), etc.
    // Status code lives inside the exception — set in the constructor.
    if (ex instanceof BusinessException biz) return biz.getStatusCode();

    // JAX-RS built-in: throw new WebApplicationException("msg", 422)
    // Used for quick HTTP errors that don't warrant their own class.
    if (ex instanceof WebApplicationException web) return web.getResponse().getStatus();

    // Anything else is unexpected — NPE, ClassCast, DB errors, etc.
    return 500;
  }

  // --- Resolution chain: exception type → error message ---
  //
  // ConstraintViolationException needs special handling because it carries
  // multiple violations (e.g., both name and price failed). We join them
  // into a single string: "Store name is required; Price cannot be negative"
  //
  // Everything else just uses ex.getMessage() which works for:
  //   - BusinessException: message set in constructor
  //   - WebApplicationException: message passed when thrown
  //   - Other: Java's default exception message
  //
  private String resolveMessage(Exception ex) {
    if (ex instanceof ConstraintViolationException cve) {
      return cve.getConstraintViolations().stream()
          .map(ConstraintViolation::getMessage)
          .collect(Collectors.joining("; "));
    }
    return ex.getMessage();
  }
}
