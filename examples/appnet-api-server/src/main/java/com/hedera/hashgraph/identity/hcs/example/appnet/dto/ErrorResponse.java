package com.hedera.hashgraph.identity.hcs.example.appnet.dto;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * DTO that represents a response body in case request processing failed.
 */
public class ErrorResponse {

  private String errorMessage;
  private String errorDetails;

  /**
   * Creates a new response body object.
   *
   * @param message Error message.
   */
  public ErrorResponse(final String message) {
    this.errorMessage = message;
  }

  /**
   * Creates a new response body object.
   *
   * @param error Exception object, error message will be extracted from this instance.
   */
  public ErrorResponse(final Throwable error) {
    this(error.getMessage(), error);
  }

  /**
   * Creates a new response body object.
   *
   * @param message Error message.
   * @param error   Exception object to provide more details about the error.
   */
  public ErrorResponse(final String message, final Throwable error) {
    this.errorMessage = message;

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    error.printStackTrace(pw);

    this.errorDetails = sw.toString();
    pw.close();
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public String getErrorDetails() {
    return errorDetails;
  }
}
