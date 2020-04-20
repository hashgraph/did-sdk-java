package com.hedera.hashgraph.identity;

import java.util.Arrays;

/**
 * Hedera networks.
 */
public enum HederaNetwork {
  MAINNET("mainnet"), TESTNET("testnet");

  /**
   * HederaNetwork name.
   */
  private String network;

  /**
   * Creates network instance.
   *
   * @param networkName The name of the Hedera network.
   */
  HederaNetwork(final String networkName) {
    this.network = networkName;
  }

  @Override
  public String toString() {
    return network;
  }

  /**
   * Resolves network name from string to {@link HederaNetwork} type.
   *
   * @param  networkName The name of the Hedera network.
   * @return             {@link HederaNetwork} type instance value for the given string.
   */
  public static HederaNetwork get(final String networkName) {
    return Arrays.stream(values())
        .filter(net -> net.network.equals(networkName)).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Invalid network name: " + networkName));
  }
}