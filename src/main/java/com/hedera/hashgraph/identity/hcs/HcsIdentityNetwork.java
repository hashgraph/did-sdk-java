package com.hedera.hashgraph.identity.hcs;

import com.google.common.base.Charsets;
import com.google.protobuf.ByteString;
import com.hedera.hashgraph.identity.DidMethodOperation;
import com.hedera.hashgraph.identity.hcs.did.HcsDid;
import com.hedera.hashgraph.identity.hcs.did.HcsDidMessage;
import com.hedera.hashgraph.identity.hcs.did.HcsDidResolver;
import com.hedera.hashgraph.identity.hcs.did.HcsDidTopicListener;
import com.hedera.hashgraph.identity.hcs.did.HcsDidTransaction;
import com.hedera.hashgraph.identity.hcs.vc.HcsVcMessage;
import com.hedera.hashgraph.identity.hcs.vc.HcsVcOperation;
import com.hedera.hashgraph.identity.hcs.vc.HcsVcStatusResolver;
import com.hedera.hashgraph.identity.hcs.vc.HcsVcTopicListener;
import com.hedera.hashgraph.identity.hcs.vc.HcsVcTransaction;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.FileContentsQuery;
import com.hedera.hashgraph.sdk.FileId;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.PrecheckStatusException;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.PublicKey;
import com.hedera.hashgraph.sdk.TopicId;
import java.util.Collection;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

/**
 * Appnet's identity network based on Hedera HCS DID method specification.
 */
public final class HcsIdentityNetwork {
  /**
   * The address book of appnet's identity network.
   */
  private AddressBook addressBook;

  /**
   * The Hedera network on which this identity network is created.
   */
  private String network;

  /**
   * Creates a new identity network instance.
   */
  private HcsIdentityNetwork() {
    // This constructor is intentionally empty. Nothing special is needed here.
  }

  /**
   * Instantiates existing identity network from a provided address book.
   *
   * @param network     The Hedera network.
   * @param addressBook The {@link AddressBook} of the identity network.
   * @return The identity network instance.
   */
  public static HcsIdentityNetwork fromAddressBook(final String network,
                                                   final AddressBook addressBook) {
    HcsIdentityNetwork result = new HcsIdentityNetwork();
    result.network = network;
    result.addressBook = addressBook;

    return result;
  }

  /**
   * Instantiates existing identity network using an address book file read from Hedera File Service.
   *
   * @param client            The Hedera network client.
   * @param network           The Hedera network.
   * @param addressBookFileId The FileID of {@link AddressBook} file stored on Hedera File Service.
   * @return The identity network instance.
   * @throws TimeoutException        In the event the client is unable to connect to Hedera in a timely fashion
   * @throws PrecheckStatusException If the query fails validation
   */
  public static HcsIdentityNetwork fromAddressBookFile(final Client client, final String network,
                                                       final FileId addressBookFileId)
          throws TimeoutException, PrecheckStatusException {

    Hbar fileContentsQueryCost = new FileContentsQuery().setFileId(addressBookFileId).getCost(client);
    final FileContentsQuery fileQuery = new FileContentsQuery().setFileId(addressBookFileId);
    fileQuery.setMaxQueryPayment(fileContentsQueryCost);

    final ByteString contents = fileQuery.execute(client);

    HcsIdentityNetwork result = new HcsIdentityNetwork();
    result.network = network;
    result.addressBook = AddressBook.fromJson(contents.toString(Charsets.UTF_8), addressBookFileId);

    return result;
  }

  /**
   * Instantiates existing identity network using a DID generated for this network.
   *
   * @param client The Hedera network client.
   * @param hcsDid The Hedera HCS DID.
   * @return The identity network instance.
   * @throws TimeoutException        In the event the client is unable to connect to Hedera in a timely fashion
   * @throws PrecheckStatusException If the query fails validation
   */
  public static HcsIdentityNetwork fromHcsDid(final Client client, final HcsDid hcsDid)
          throws TimeoutException, PrecheckStatusException {

    final FileId addressBookFileId = hcsDid.getAddressBookFileId();
    return HcsIdentityNetwork.fromAddressBookFile(client, hcsDid.getNetwork(), addressBookFileId);
  }

  /**
   * Instantiates a {@link HcsDidTransaction} to perform the specified operation on the DID document.
   *
   * @param operation The operation to be performed on a DID document.
   * @return The {@link HcsDidTransaction} instance.
   */
  public HcsDidTransaction createDidTransaction(final DidMethodOperation operation) {
    return new HcsDidTransaction(operation, getDidTopicId());
  }

  /**
   * Instantiates a {@link HcsDidTransaction} to perform the specified operation on the DID document.
   *
   * @param message The DID topic message ready to for sending.
   * @return The {@link HcsDidTransaction} instance.
   */
  public HcsDidTransaction createDidTransaction(final MessageEnvelope<HcsDidMessage> message) {
    return new HcsDidTransaction(message, getDidTopicId());
  }

  /**
   * Instantiates a {@link HcsVcTransaction} to perform the specified operation on the VC document.
   *
   * @param operation       The type of operation.
   * @param credentialHash  Credential hash.
   * @param signerPublicKey Public key of the signer (issuer).
   * @return The transaction instance.
   */
  public HcsVcTransaction createVcTransaction(final HcsVcOperation operation, final String credentialHash,
                                              final PublicKey signerPublicKey) {
    return new HcsVcTransaction(getVcTopicId(), operation, credentialHash, signerPublicKey);
  }

  /**
   * Instantiates a {@link HcsVcTransaction} to perform the specified operation on the VC document status.
   *
   * @param message         The VC topic message ready to for sending.
   * @param signerPublicKey Public key of the signer (usually issuer).
   * @return The {@link HcsVcTransaction} instance.
   */
  public HcsVcTransaction createVcTransaction(final MessageEnvelope<HcsVcMessage> message,
                                              final PublicKey signerPublicKey) {
    return new HcsVcTransaction(getVcTopicId(), message, signerPublicKey);
  }

  /**
   * Returns the address book of this identity network.
   *
   * @return The address book of this identity network.
   */
  public AddressBook getAddressBook() {
    return addressBook;
  }

  /**
   * Returns the Hedera network on which this identity network runs.
   *
   * @return The Hedera network.
   */
  public String getNetwork() {
    return network;
  }

  /**
   * Generates a new DID and it's root key.
   *
   * @param withTid Indicates if DID topic ID should be added to the DID as <i>tid</i> parameter.
   * @return Generated {@link HcsDid} with it's private DID root key.
   */
  public HcsDid generateDid(final boolean withTid) {
    PrivateKey privateKey = HcsDid.generateDidRootKey();
    TopicId tid = withTid ? getDidTopicId() : null;

    return new HcsDid(getNetwork(), privateKey, addressBook.getFileId(), tid);
  }

  /**
   * Generates a new DID from the given public DID root key.
   *
   * @param publicKey A DID root key.
   * @param withTid   Indicates if DID topic ID should be added to the DID as <i>tid</i> parameter.
   * @return A newly generated DID.
   */
  public HcsDid generateDid(final PublicKey publicKey, final boolean withTid) {
    TopicId tid = withTid ? getDidTopicId() : null;
    return new HcsDid(getNetwork(), publicKey, getAddressBook().getFileId(), tid);
  }

  /**
   * Returns a DID resolver for this network.
   *
   * @return The DID resolver for this network.
   */
  public HcsDidResolver getDidResolver() {
    return new HcsDidResolver(getDidTopicId());
  }

  /**
   * Returns DID topic ID for this network.
   *
   * @return The DID topic ID.
   */
  public TopicId getDidTopicId() {
    return TopicId.fromString(addressBook.getDidTopicId());
  }

  /**
   * Returns a DID topic listener for this network.
   *
   * @return The DID topic listener.
   */
  public HcsDidTopicListener getDidTopicListener() {
    return new HcsDidTopicListener(getDidTopicId());
  }

  /**
   * Returns Verifiable Credentials topic ID for this network.
   *
   * @return The VC topic ID.
   */
  public TopicId getVcTopicId() {
    return TopicId.fromString(addressBook.getVcTopicId());
  }

  /**
   * Returns a VC status resolver for this network.
   *
   * @return The VC status resolver for this network.
   */
  public HcsVcStatusResolver getVcStatusResolver() {
    return new HcsVcStatusResolver(getVcTopicId());
  }

  /**
   * Returns a VC status resolver for this network.
   * Resolver will validate signatures of topic messages against public keys supplied
   * by the given provider.
   *
   * @param publicKeysProvider Provider of a public keys acceptable for a given VC hash.
   * @return The VC status resolver for this network.
   */
  public HcsVcStatusResolver getVcStatusResolver(
          final Function<String, Collection<PublicKey>> publicKeysProvider) {
    return new HcsVcStatusResolver(getVcTopicId(), publicKeysProvider);
  }

  /**
   * Returns a VC topic listener for this network.
   *
   * @return The VC topic listener.
   */
  public HcsVcTopicListener getVcTopicListener() {
    return new HcsVcTopicListener(getVcTopicId());
  }

  /**
   * Returns a VC topic listener for this network.
   * This listener will validate signatures of topic messages against public keys supplied
   * by the given provider.
   *
   * @param publicKeysProvider Provider of a public keys acceptable for a given VC hash.
   * @return The VC topic listener.
   */
  public HcsVcTopicListener getVcTopicListener(
          final Function<String, Collection<PublicKey>> publicKeysProvider) {
    return new HcsVcTopicListener(getVcTopicId(), publicKeysProvider);
  }
}
