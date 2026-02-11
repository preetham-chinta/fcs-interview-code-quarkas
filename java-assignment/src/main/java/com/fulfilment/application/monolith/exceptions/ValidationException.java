package com.fulfilment.application.monolith.exceptions;

/** Thrown when input fails business validation rules. Maps to HTTP 400. */
public class ValidationException extends BusinessException {

  public ValidationException(String message) {
    super(message, 400);
  }
}
