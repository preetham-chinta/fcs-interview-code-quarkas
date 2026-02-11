package com.fulfilment.application.monolith.exceptions;

/** Thrown when a requested entity does not exist. Maps to HTTP 404. */
public class NotFoundException extends BusinessException {

  public NotFoundException(String entity, Object identifier) {
    super(entity + " with id of " + identifier + " does not exist.", 404);
  }

  /** Flexible constructor for custom not-found messages (e.g., lookup by business unit code). */
  public NotFoundException(String message) {
    super(message, 404);
  }
}
