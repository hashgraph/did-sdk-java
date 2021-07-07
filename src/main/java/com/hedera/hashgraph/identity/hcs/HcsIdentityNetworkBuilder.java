package com.hedera.hashgraph.identity.hcs;

import com.google.common.base.Charsets;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.FileCreateTransaction;
import com.hedera.hashgraph.sdk.FileId;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.PrecheckStatusException;
import com.hedera.hashgraph.sdk.PublicKey;
import com.hedera.hashgraph.sdk.ReceiptStatusException;
import com.hedera.hashgraph.sdk.TopicCreateTransaction;
import com.hedera.hashgraph.sdk.TopicId;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.sdk.TransactionResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * The builder used to create new appnet identity networks based on Hedera HCS DID method specification.
 */
public class HcsIdentityNetworkBuilder {
  private String appnetName;
  private String network;
  private List<String> didServers;
  private PublicKey publicKey;
  private Hbar maxTransactionFee = Hbar.from(2);
  private String didTopicMemo = "";
  private String vcTopicMemo = "";

  /**
   * Creates a new identity network for the given appnet.
   * This will initialize an address book file on Hedera File Service.
   * Then DID and VC topics on Hedera network will be created unless already existing topics are provided.
   *
   * @param client Hedera client
   * @return The new identity network.
   * @throws ReceiptStatusException  in the event the receipt contains an error
   * @throws PrecheckStatusException in the event the transaction isn't validated by the network
   * @throws TimeoutException        in the event the client fails to communicate with the network in a timely fashion
   */
  public HcsIdentityNetwork execute(final Client client)
          throws ReceiptStatusException, PrecheckStatusException, TimeoutException {

    TopicCreateTransaction didTopicCreateTransaction = new TopicCreateTransaction()
            .setMaxTransactionFee(maxTransactionFee)
            .setTopicMemo(didTopicMemo);
    if (publicKey != null) {
      didTopicCreateTransaction.setAdminKey(publicKey);
    }

    TransactionResponse didTxId = didTopicCreateTransaction
            .execute(client);
    TopicId didTopicId = didTxId.getReceipt(client).topicId;

    TopicCreateTransaction vcTopicCreateTransaction = new TopicCreateTransaction()
            .setMaxTransactionFee(maxTransactionFee)
            .setTopicMemo(vcTopicMemo);
    if (publicKey != null) {
      vcTopicCreateTransaction.setAdminKey(publicKey);
    }

    TransactionResponse vcTxId = vcTopicCreateTransaction.execute(client);
    TopicId vcTopicId = vcTxId.getReceipt(client).topicId;

    AddressBook addressBook = AddressBook
            .create(appnetName, didTopicId.toString(), vcTopicId.toString(), didServers);

    FileCreateTransaction fileCreateTx = new FileCreateTransaction()
            .setContents(addressBook.toJson().getBytes(Charsets.UTF_8));

    TransactionResponse response = fileCreateTx.execute(client);
    TransactionReceipt receipt = response.getReceipt(client);
    FileId fileId = receipt.fileId;

    addressBook.setFileId(fileId);

    return HcsIdentityNetwork.fromAddressBook(network, addressBook);
  }

  /**
   * Adds an appnet server URL that hosts DID REST API as specified by Hedera HCS DID method.
   *
   * @param serverUrl The URL to the appnet's server DID REST service.
   * @return This identity network builder instance.
   */
  public HcsIdentityNetworkBuilder addAppnetDidServer(final String serverUrl) {
    if (didServers == null) {
      didServers = new ArrayList<>(1);
    }

    if (!didServers.contains(serverUrl)) {
      didServers.add(serverUrl);
    }

    return this;
  }

  /**
   * Defines the name of the appnet for identity network address book.
   *
   * @param appnetName The name of the appnet.
   * @return This identity network builder instance.
   */
  public HcsIdentityNetworkBuilder setAppnetName(final String appnetName) {
    this.appnetName = appnetName;
    return this;
  }

  /**
   * Defines the memo for the DiD Topic.
   *
   * @param didTopicMemo The memo for the DiD Topic
   * @return This identity network builder instance.
   */
  public HcsIdentityNetworkBuilder setDidTopicMemo(final String didTopicMemo) {
    this.didTopicMemo = didTopicMemo;
    return this;
  }

  /**
   * Defines the memo for the VC Topic.
   *
   * @param vcTopicMemo The memo for the VC Topic
   * @return This identity network builder instance.
   */
  public HcsIdentityNetworkBuilder setVCTopicMemo(final String vcTopicMemo) {
    this.vcTopicMemo = vcTopicMemo;
    return this;
  }

  /**
   * Sets the max fee for transactions.
   *
   * @param maxTransactionFee The max transaction fee in hBar
   * @return This identity network builder instance.
   */
  public HcsIdentityNetworkBuilder setMaxTransactionFee(final Hbar maxTransactionFee) {
    this.maxTransactionFee = maxTransactionFee;
    return this;
  }

  /**
   * Sets the key for topic submission.
   *
   * @param publicKey The publicKey to use.
   * @return This identity network builder instance.
   */
  public HcsIdentityNetworkBuilder setPublicKey(final PublicKey publicKey) {
    this.publicKey = publicKey;
    return this;
  }

  /**
   * Defines the Hedera Network on which the identities and credentials are registered.
   *
   * @param network The network to set.
   * @return This identity network builder instance.
   */
  public HcsIdentityNetworkBuilder setNetwork(final String network) {
    this.network = network;
    return this;
  }
}
