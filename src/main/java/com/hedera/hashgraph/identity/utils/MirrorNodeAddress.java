package com.hedera.hashgraph.identity.utils;

import io.github.cdimascio.dotenv.Dotenv;
import java.util.Objects;

/**
 * Calculates mirror node address from environment variables.
 */
public final class MirrorNodeAddress {

  private MirrorNodeAddress() {
  }

  /**
   * return mirror node address from environment variables.
   *
   * @return {@link String} mirror address.
   */
  public static String getAddress() {
    Dotenv dotenv = Dotenv.configure().ignoreIfMissing().ignoreIfMalformed().load();
    // Grab the desired mirror from environment variables
    final String mirrorProvider = Objects.requireNonNull(dotenv.get("MIRROR_PROVIDER"));
    // Grab the network to use from environment variables
    final String network = Objects.requireNonNull(dotenv.get("NETWORK"));
    return getAddress(network, mirrorProvider);
  }

  /**
   * return mirror node address from supplied values.
   *
   * @param network        the network to use (mainnet, testnet, ...)
   * @param mirrorProvider the mirror provider (hedera or kabuto)
   * @return {@link String} mirror address.
   */
  public static String getAddress(final String network, final String mirrorProvider) {
    String mirrorNodeAddress = "hcs." + network + ".mirrornode.hedera.com:5600";
    if ("kabuto".equals(mirrorProvider)) {
      switch (network) {
        case "mainnet":
          mirrorNodeAddress = "api.kabuto.sh:50211";
          break;
        case "testnet":
          mirrorNodeAddress = "api.testnet.kabuto.sh:50211";
          break;
        default:
          System.exit(1);
          break;
      }
    }
    return mirrorNodeAddress;
  }
}
