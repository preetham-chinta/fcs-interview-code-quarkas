package com.fulfilment.application.monolith.exceptions;

import java.time.LocalDateTime;
import java.util.UUID;

/** Standardized error response body returned by all endpoints. */
public class ErrorResponse {

  public String errorId;
  public int code;
  public String error;
  public String exceptionType;
  public LocalDateTime timestamp;

  public ErrorResponse() {}

  public ErrorResponse(int code, String error, String exceptionType) {
    this.errorId = UUID.randomUUID().toString().substring(0, 8);
    this.code = code;
    this.error = error;
    this.exceptionType = exceptionType;
    this.timestamp = LocalDateTime.now();
  }
}
