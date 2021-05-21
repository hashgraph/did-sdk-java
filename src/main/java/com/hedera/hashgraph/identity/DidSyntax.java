package com.hedera.hashgraph.identity;

import java.util.Arrays;

/**
 * Hedera DID method syntax.
 */
public final class DidSyntax {
  /**
   * DID prefix as defined by W3C DID specification.
   */
  public static final String DID_PREFIX = "did";

  public static final String DID_DOCUMENT_CONTEXT = "https://www.w3.org/ns/did/v1";
  public static final String DID_METHOD_SEPARATOR = ":";
  public static final String DID_PARAMETER_SEPARATOR = ";";
  public static final String DID_PARAMETER_VALUE_SEPARATOR = "=";

  /**
   * This class is not to be instantiated.
   */
  private DidSyntax() {
    // Empty on purpose.
  }

  /**
   * Hedera DID Method.
   */
  public enum Method {
    HEDERA_HCS("hedera");

    /**
     * Method name.
     */
    private String method;

    /**
     * Creates Method instance.
     *
     * @param methodName The name of the Hedera DID method.
     */
    Method(final String methodName) {
      this.method = methodName;
    }

    /**
     * Resolves method name from string to {@link Method} type.
     *
     * @param methodName The name of the Hedera method.
     * @return {@link Method} type instance value for the given string.
     */
    public static Method get(final String methodName) {
      return Arrays.stream(values())
              .filter(m -> m.method.equals(methodName)).findFirst()
              .orElseThrow(() -> new IllegalArgumentException("Invalid DID method name: " + methodName));
    }

    @Override
    public String toString() {
      return method;
    }
  }

  /**
   * Hedera DID method-specific URL Parameters.
   */
  public static final class MethodSpecificParameter {
    /**
     * MethodSpecificParameter name for the FileId of appnet's address book.
     */
    public static final String ADDRESS_BOOK_FILE_ID = "fid";

    /**
     * MethodSpecificParameter name for the TopicId of appnet's DID topic.
     */
    public static final String DID_TOPIC_ID = "tid";

    /**
     * This class is not to be instantiated.
     */
    private MethodSpecificParameter() {
      // Empty on purpose.
    }
  }
}
