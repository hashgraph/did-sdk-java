package com.hedera.hashgraph.identity.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * {@link Validator} allows implementing classes to run validation checks and gather all errors.
 */
public class Validator {
  protected List<String> validationErrors;

  /**
   * Adds a validation error to the errors list.
   *
   * @param errorMessage The error message.
   */
  public void addValidationError(final String errorMessage) {
    if (validationErrors == null) {
      validationErrors = new ArrayList<>();
    }

    validationErrors.add(errorMessage);
  }

  /**
   * Checks if there are any validation errors and if yes throws {@link IllegalStateException}.
   *
   * @param prologue           A prologue before the errors list.
   * @param validationFunction Validation function that defines all check conditions.
   */
  public void checkValidationErrors(final String prologue, final Consumer<Validator> validationFunction) {
    validationErrors = null;

    validationFunction.accept(this);

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
  public final void require(final boolean condition, final String errorMessage) {
    if (!condition) {
      addValidationError(errorMessage);
    }
  }
}
