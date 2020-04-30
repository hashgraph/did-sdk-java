package com.hedera.hashgraph.identity.hcs.example.appnet.handlers;

import com.hedera.hashgraph.identity.hcs.HcsIdentityNetwork;
import com.hedera.hashgraph.identity.hcs.example.appnet.AppnetStorage;

/**
 * A base class for all appnet handlers that require access to local storage and identity network.
 */
public abstract class AppnetHandler {

  protected HcsIdentityNetwork identityNetwork;
  protected AppnetStorage storage;

  /**
   * Creates a new appnet request handler instance.
   *
   * @param identityNetwork The Hedera Identity network.
   * @param storage         The appnet's local storage.
   */
  protected AppnetHandler(final HcsIdentityNetwork identityNetwork, final AppnetStorage storage) {
    this.identityNetwork = identityNetwork;
    this.storage = storage;
  }
}
