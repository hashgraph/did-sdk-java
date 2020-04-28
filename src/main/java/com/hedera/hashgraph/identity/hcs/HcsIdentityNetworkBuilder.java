package com.hedera.hashgraph.identity.hcs;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.hedera.hashgraph.identity.HederaNetwork;
import com.hedera.hashgraph.identity.utils.Validator;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.HederaNetworkException;
import com.hedera.hashgraph.sdk.HederaStatusException;
import com.hedera.hashgraph.sdk.Transaction;
import com.hedera.hashgraph.sdk.TransactionId;
import com.hedera.hashgraph.sdk.consensus.ConsensusTopicCreateTransaction;
import com.hedera.hashgraph.sdk.consensus.ConsensusTopicId;
import com.hedera.hashgraph.sdk.file.FileCreateTransaction;
import com.hedera.hashgraph.sdk.file.FileId;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * The builder used to create new appnet identity networks based on Hedera HCS DID method specification.
 */
public class HcsIdentityNetworkBuilder {
  private String appnetName;
  private ConsensusTopicId didTopicId;
  private ConsensusTopicId vcTopicId;
  private HederaNetwork network;
  private Function<ConsensusTopicCreateTransaction, Transaction> didTopicTxFunction;
  private Function<ConsensusTopicCreateTransaction, Transaction> vcTopicTxFunction;
  private Function<FileCreateTransaction, Transaction> addressBookTxFunction;
  private List<String> didServers;

  /**
   * Creates a new identity network for the given appnet.
   * This will initialize an address book file on Hedera File Service.
   * Then DID and VC topics on Hedera network will be created unless already existing topics are provided.
   *
   * @param  client                 Hedera client
   * @return                        The new identity network.
   * @throws HederaStatusException  In case querying Hedera File Service fails.
   * @throws HederaNetworkException In case of querying Hedera File Service fails due to transport calls.
   */
  public HcsIdentityNetwork execute(final Client client) throws HederaNetworkException, HederaStatusException {
    new Validator().checkValidationErrors("HederaNetwork not created: ", v -> validate(v));

    if (didTopicTxFunction != null) {
      TransactionId didTxId = didTopicTxFunction.apply(new ConsensusTopicCreateTransaction()).execute(client);
      didTopicId = didTxId.getReceipt(client).getConsensusTopicId();
      didTopicTxFunction = null;
    }

    if (vcTopicTxFunction != null) {
      TransactionId vcTxId = vcTopicTxFunction.apply(new ConsensusTopicCreateTransaction()).execute(client);
      vcTopicId = vcTxId.getReceipt(client).getConsensusTopicId();
      vcTopicTxFunction = null;
    }

    // Create address book file.
    AddressBook addressBook = AddressBook
        .create(appnetName, didTopicId.toString(), vcTopicId.toString(), didServers);

    FileCreateTransaction fileCreateTx = new FileCreateTransaction()
        .setContents(addressBook.toJson().getBytes(Charsets.UTF_8));

    Transaction tx = addressBookTxFunction.apply(fileCreateTx);
    FileId fileId = tx.execute(client)
        .getReceipt(client)
        .getFileId();

    addressBook.setFileId(fileId);

    return HcsIdentityNetwork.fromAddressBook(network, addressBook);
  }

  /**
   * Creates and configures a new {@link ConsensusTopicCreateTransaction} for DID document messages topic in HCS.
   * The transaction is not executed here, only upon calling execute method of the builder.
   *
   * @param  builder The transaction builder that shall set all transaction properties, build and sign it.
   * @return         This identity network builder instance.
   */
  public HcsIdentityNetworkBuilder buildAndSignDidTopicCreateTransaction(
      final Function<ConsensusTopicCreateTransaction, Transaction> builder) {
    didTopicTxFunction = builder;
    return this;
  }

  /**
   * Creates and configures a new {@link ConsensusTopicCreateTransaction} for Verifiable Credentials topic in HCS.
   * The transaction is not executed here, only upon calling execute method of the builder.
   *
   * @param  builder The transaction builder that shall set all transaction properties, build and sign it.
   * @return         This identity network builder instance.
   */
  public HcsIdentityNetworkBuilder buildAndSignVcTopicCreateTransaction(
      final Function<ConsensusTopicCreateTransaction, Transaction> builder) {
    vcTopicTxFunction = builder;
    return this;
  }

  /**
   * Creates and configures a new {@link FileCreateTransaction} for appnet's address book file.
   * The transaction is not executed here, only upon calling execute method of the builder.
   * File content is already set.
   *
   * @param  builder The transaction builder that shall configure, build and sign given {@link FileCreateTransaction}.
   * @return         This identity network builder instance.
   */
  public HcsIdentityNetworkBuilder buildAndSignAddressBookCreateTransaction(
      final Function<FileCreateTransaction, Transaction> builder) {
    addressBookTxFunction = builder;
    return this;
  }

  /**
   * Adds an appnet server URL that hosts DID REST API as specified by Hedera HCS DID method.
   *
   * @param  serverUrl The URL to the appnet's server DID REST service.
   * @return           This identity network builder instance.
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
   * Runs validation logic.
   *
   * @param validator The errors validator.
   */
  protected void validate(final Validator validator) {
    validator.require(didTopicTxFunction != null || didTopicId != null,
        "Provide an existing DID TopicId or build and sign TopicCreateTransaction.");
    validator.require(vcTopicTxFunction != null || vcTopicId != null,
        "Provide an existing Verifiable Credentials TopicId or  build and sign TopicCreateTransaction.");

    validator.require(!(didTopicTxFunction != null && didTopicId != null),
        "Provide an existing DID TopicId or build and sign  TopicCreateTransaction, but not both.");
    validator.require(!(vcTopicTxFunction != null && vcTopicId != null),
        "Provide an existing Verifiable Credentials TopicId or build and sign TopicCreateTransaction, but not both.");

    validator.require(addressBookTxFunction != null && !Strings.isNullOrEmpty(appnetName),
        "Build and sign FileCreateTransaction for address book file.");

    validator.require(network != null, "HederaNetwork is not defined.");

    validator.require(didServers != null && !didServers.isEmpty(), "No Appnet DID servers are specified.");
  }

  /**
   * Defines the name of the appnet for identity network address book.
   *
   * @param  appnetName The name of the appnet.
   * @return            This identity network builder instance.
   */
  public HcsIdentityNetworkBuilder setAppnetName(final String appnetName) {
    this.appnetName = appnetName;
    return this;
  }

  /**
   * Sets existing HCS Topic ID to be used for DID Document messages.
   *
   * @param  didTopicId The DID {@link ConsensusTopicId} to set.
   * @return            This identity network builder instance.
   */
  public HcsIdentityNetworkBuilder setDidTopicId(final ConsensusTopicId didTopicId) {
    this.didTopicId = didTopicId;
    return this;
  }

  /**
   * Sets existing HCS Topic ID to be used for Verifiable Credentials messages.
   *
   * @param  vcTopicId The Verifiable Credentials {@link ConsensusTopicId} to set.
   * @return           This identity network builder instance.
   */
  public HcsIdentityNetworkBuilder setVCTopicId(final ConsensusTopicId vcTopicId) {
    this.vcTopicId = vcTopicId;
    return this;
  }

  /**
   * Defines the Hedera Network on which the identities and credentials are registered.
   *
   * @param  network The network to set.
   * @return         This identity network builder instance.
   */
  public HcsIdentityNetworkBuilder setNetwork(final HederaNetwork network) {
    this.network = network;
    return this;
  }
}
