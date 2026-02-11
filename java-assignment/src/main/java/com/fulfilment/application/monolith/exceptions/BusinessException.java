package com.fulfilment.application.monolith.exceptions;

/**
 * Base class for all domain/business exceptions.
 * Subclasses define specific error types; ExceptionMappers handle the HTTP translation.
 */
public abstract class BusinessException extends RuntimeException {

  private final int statusCode;

  protected BusinessException(String message, int statusCode) {
    super(message);
    this.statusCode = statusCode;
  }

  public int getStatusCode() {
    return statusCode;
  }
}
