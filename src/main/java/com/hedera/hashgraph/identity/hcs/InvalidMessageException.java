package com.hedera.hashgraph.identity.hcs;

import com.hedera.hashgraph.sdk.TopicMessage;

/**
 * Exception thrown when the message coming from the mirror node fails validation.
 */
public class InvalidMessageException extends Exception {
  private static final long serialVersionUID = -2493756740875603220L;
  private TopicMessage mirrorResponse;

  public InvalidMessageException(final String reason) {
    super(reason);
  }

  public InvalidMessageException(final TopicMessage response, final String reason) {
    super(reason);
    this.mirrorResponse = response;
  }

  public TopicMessage getMirrorResponse() {
    return mirrorResponse;
  }
}
