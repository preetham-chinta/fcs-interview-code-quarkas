package com.fulfilment.application.monolith.exceptions;

/** Thrown when an operation conflicts with current state (e.g., duplicate). Maps to HTTP 409. */
public class ConflictException extends BusinessException {

  public ConflictException(String message) {
    super(message, 409);
  }
}
