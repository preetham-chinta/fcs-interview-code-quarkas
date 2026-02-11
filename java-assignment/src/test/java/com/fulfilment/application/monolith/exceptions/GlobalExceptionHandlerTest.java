package com.fulfilment.application.monolith.exceptions;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for GlobalExceptionHandler — the centralized @Provider ExceptionMapper.
 *
 * Verifies the resolution chain: ConstraintViolationException → BusinessException
 * → WebApplicationException → fallback 500. Each branch produces the correct
 * HTTP status code and ErrorResponse body.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

  @InjectMocks private GlobalExceptionHandler handler;

  @Nested
  @DisplayName("BusinessException handling")
  class BusinessExceptions {

    @Test
    @DisplayName("should map NotFoundException to 404 with correct message")
    void shouldMapNotFoundExceptionTo404() {
      // given
      NotFoundException ex = new NotFoundException("Store", 42L);

      // when
      Response response = handler.toResponse(ex);

      // then
      assertEquals(404, response.getStatus());
      ErrorResponse body = (ErrorResponse) response.getEntity();
      assertEquals(404, body.code);
      assertTrue(body.error.contains("Store with id of 42"));
      assertTrue(body.error.contains("does not exist"));
      assertEquals(NotFoundException.class.getName(), body.exceptionType);
      assertNotNull(body.errorId);
      assertNotNull(body.timestamp);
    }

    @Test
    @DisplayName("should map ValidationException to 400")
    void shouldMapValidationExceptionTo400() {
      // given
      ValidationException ex = new ValidationException("Name is required");

      // when
      Response response = handler.toResponse(ex);

      // then
      assertEquals(400, response.getStatus());
      ErrorResponse body = (ErrorResponse) response.getEntity();
      assertEquals(400, body.code);
      assertEquals("Name is required", body.error);
    }

    @Test
    @DisplayName("should map ConflictException to 409")
    void shouldMapConflictExceptionTo409() {
      // given
      ConflictException ex = new ConflictException("Resource already exists");

      // when
      Response response = handler.toResponse(ex);

      // then
      assertEquals(409, response.getStatus());
      ErrorResponse body = (ErrorResponse) response.getEntity();
      assertEquals(409, body.code);
      assertEquals("Resource already exists", body.error);
    }
  }

  @Nested
  @DisplayName("ConstraintViolationException handling")
  class ConstraintViolations {

    @Test
    @DisplayName("should map ConstraintViolationException to 400 with joined messages")
    void shouldMapConstraintViolationTo400() {
      // given — mock a ConstraintViolation
      @SuppressWarnings("unchecked")
      ConstraintViolation<Object> violation = Mockito.mock(ConstraintViolation.class);
      Mockito.when(violation.getMessage()).thenReturn("Store name is required");

      ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));

      // when
      Response response = handler.toResponse(ex);

      // then
      assertEquals(400, response.getStatus());
      ErrorResponse body = (ErrorResponse) response.getEntity();
      assertEquals(400, body.code);
      assertTrue(body.error.contains("Store name is required"));
    }

    @Test
    @DisplayName("should join multiple violation messages with semicolon")
    void shouldJoinMultipleViolationMessages() {
      // given
      @SuppressWarnings("unchecked")
      ConstraintViolation<Object> v1 = Mockito.mock(ConstraintViolation.class);
      @SuppressWarnings("unchecked")
      ConstraintViolation<Object> v2 = Mockito.mock(ConstraintViolation.class);
      Mockito.when(v1.getMessage()).thenReturn("Name is required");
      Mockito.when(v2.getMessage()).thenReturn("Stock cannot be negative");

      ConstraintViolationException ex = new ConstraintViolationException(Set.of(v1, v2));

      // when
      Response response = handler.toResponse(ex);

      // then
      assertEquals(400, response.getStatus());
      ErrorResponse body = (ErrorResponse) response.getEntity();
      // Set ordering is non-deterministic, so check both messages are present
      assertTrue(body.error.contains("Name is required"));
      assertTrue(body.error.contains("Stock cannot be negative"));
      assertTrue(body.error.contains("; ")); // joined by semicolon
    }
  }

  @Nested
  @DisplayName("WebApplicationException handling")
  class WebApplicationExceptions {

    @Test
    @DisplayName("should preserve status code from WebApplicationException")
    void shouldPreserveStatusCode() {
      // given
      WebApplicationException ex =
          new WebApplicationException("Id was invalidly set on request.", 422);

      // when
      Response response = handler.toResponse(ex);

      // then
      assertEquals(422, response.getStatus());
      ErrorResponse body = (ErrorResponse) response.getEntity();
      assertEquals(422, body.code);
      assertEquals("Id was invalidly set on request.", body.error);
    }

    @Test
    @DisplayName("should handle 409 Conflict from WebApplicationException")
    void shouldHandle409Conflict() {
      // given
      WebApplicationException ex =
          new WebApplicationException("Warehouse already exists.", 409);

      // when
      Response response = handler.toResponse(ex);

      // then
      assertEquals(409, response.getStatus());
      ErrorResponse body = (ErrorResponse) response.getEntity();
      assertEquals(409, body.code);
    }
  }

  @Nested
  @DisplayName("Unhandled exception fallback")
  class UnhandledExceptions {

    @Test
    @DisplayName("should map unknown exceptions to 500 Internal Server Error")
    void shouldMapUnknownExceptionTo500() {
      // given
      RuntimeException ex = new RuntimeException("Something went wrong");

      // when
      Response response = handler.toResponse(ex);

      // then
      assertEquals(500, response.getStatus());
      ErrorResponse body = (ErrorResponse) response.getEntity();
      assertEquals(500, body.code);
      assertEquals("Something went wrong", body.error);
      assertEquals(RuntimeException.class.getName(), body.exceptionType);
    }

    @Test
    @DisplayName("should map NullPointerException to 500")
    void shouldMapNullPointerExceptionTo500() {
      // given
      NullPointerException ex = new NullPointerException("null reference");

      // when
      Response response = handler.toResponse(ex);

      // then
      assertEquals(500, response.getStatus());
      ErrorResponse body = (ErrorResponse) response.getEntity();
      assertEquals(500, body.code);
    }
  }

  @Nested
  @DisplayName("ErrorResponse structure")
  class ErrorResponseStructure {

    @Test
    @DisplayName("should include errorId, code, error, exceptionType, and timestamp")
    void shouldIncludeAllFields() {
      // given
      NotFoundException ex = new NotFoundException("Product", 7L);

      // when
      Response response = handler.toResponse(ex);

      // then
      ErrorResponse body = (ErrorResponse) response.getEntity();
      assertNotNull(body.errorId, "errorId should be generated");
      assertFalse(body.errorId.isEmpty(), "errorId should not be empty");
      assertEquals(404, body.code);
      assertNotNull(body.error);
      assertEquals(NotFoundException.class.getName(), body.exceptionType);
      assertNotNull(body.timestamp, "timestamp should be set");
    }
  }
}
