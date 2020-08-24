package com.hedera.hashgraph.identity.utils;
import io.github.cdimascio.dotenv.Dotenv;
import java.io.IOException;
import java.util.Objects;

/**
 * Calculates mirror node address from environment variables
 */
public class MirrorNodeAddress {

  /**
   * return mirror node address from environment variables
   *
   * @return                          {@link String} mirror address.
   */
  public static String getAddress() throws IOException {
    Dotenv dotenv = Dotenv.configure().ignoreIfMissing().ignoreIfMalformed().load();
    // Grab the desired mirror from environment variables
    final String mirrorProvider = Objects.requireNonNull(dotenv.get("MIRROR_PROVIDER"));
    // Grab the network to use from environment variables
    final String network = Objects.requireNonNull(dotenv.get("NETWORK"));

    String mirrorNodeAddress = "hcs." + network + ".mirrornode.hedera.com:5600";
    if (mirrorProvider.equals("kabuto")) {
      switch (network) {
        case "mainnet":
          mirrorNodeAddress = "api.kabuto.sh:50211";
          break;
        case "testnet":
          mirrorNodeAddress = "api.testnet.kabuto.sh:50211";
          break;
        case "previewnet":
          System.out.println("invalid previewnet network for Kabuto, please edit .env file");
          System.exit(1);
      }
    }
    return mirrorNodeAddress;
  }
}
