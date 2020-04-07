package com.hedera.hashgraph.identity.hcs;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link TransactionValidator} allows Transaction builder run validation checks and gather all errors.
 */
public abstract class TransactionValidator {
  protected List<String> validationErrors;

  /**
   * Adds a validation error to the errors list.
   *
   * @param errorMessage The error message.
   */
  protected void addValidationError(final String errorMessage) {
    if (validationErrors == null) {
      validationErrors = new ArrayList<>();
    }

    validationErrors.add(errorMessage);
  }

  /**
   * Checks if there are any validation errors and if yes throws {@link IllegalStateException}.
   *
   * @param prologue A prologue before the errors list.
   */
  protected void checkValidationErrors(final String prologue) {
    validationErrors = null;

    validate();

    if (validationErrors == null) {
      return;
    }

    List<String> errors = validationErrors;
    validationErrors = null;

    throw new IllegalStateException(prologue + ":\n" + String.join("\n", errors));
  }

  /**
   * Checks if required condition is true and if not adds the given error message to the list.
   *
   * @param condition    The condition to be met.
   * @param errorMessage The error message in case condition is not met.
   */
  protected final void require(final boolean condition, final String errorMessage) {
    if (!condition) {
      addValidationError(errorMessage);
    }
  }

  /**
   * Runs validation logic. Throws child-class specific runtime exceptions if validation fails.
   */
  protected abstract void validate();

}
