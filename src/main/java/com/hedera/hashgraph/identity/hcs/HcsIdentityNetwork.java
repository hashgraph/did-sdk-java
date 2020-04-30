package com.hedera.hashgraph.identity.hcs;

import com.google.common.base.Charsets;
import com.hedera.hashgraph.identity.DidMethodOperation;
import com.hedera.hashgraph.identity.HederaNetwork;
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
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.HederaNetworkException;
import com.hedera.hashgraph.sdk.HederaStatusException;
import com.hedera.hashgraph.sdk.consensus.ConsensusTopicId;
import com.hedera.hashgraph.sdk.crypto.ed25519.Ed25519PrivateKey;
import com.hedera.hashgraph.sdk.crypto.ed25519.Ed25519PublicKey;
import com.hedera.hashgraph.sdk.file.FileContentsQuery;
import com.hedera.hashgraph.sdk.file.FileId;
import java.security.SecureRandom;
import java.util.Collection;
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
  private HederaNetwork network;

  /**
   * Creates a new identity network instance.
   */
  private HcsIdentityNetwork() {
    // This constructor is intentionally empty. Nothing special is needed here.
  }

  /**
   * Instantiates existing identity network from a provided address book.
   *
   * @param  network     The Hedera network.
   * @param  addressBook The {@link AddressBook} of the identity network.
   * @return             The identity network instance.
   */
  public static HcsIdentityNetwork fromAddressBook(final HederaNetwork network,
      final AddressBook addressBook) {
    HcsIdentityNetwork result = new HcsIdentityNetwork();
    result.network = network;
    result.addressBook = addressBook;

    return result;
  }

  /**
   * Instantiates existing identity network using an address book file read from Hedera File Service.
   *
   * @param  client                 The Hedera network client.
   * @param  network                The Hedera network.
   * @param  addressBookFileId      The FileID of {@link AddressBook} file stored on Hedera File Service.
   * @param  maxQueryPayment        Maximum HBAR payment for querying the address book file content.
   * @return                        The identity network instance.
   * @throws HederaStatusException  In case querying Hedera File Service fails.
   * @throws HederaNetworkException In case of querying Hedera File Service fails due to transport calls.
   */
  public static HcsIdentityNetwork fromAddressBookFile(final Client client, final HederaNetwork network,
      final FileId addressBookFileId,
      final Hbar maxQueryPayment)
      throws HederaNetworkException, HederaStatusException {

    final FileContentsQuery fileQuery = new FileContentsQuery().setFileId(addressBookFileId);
    fileQuery.setMaxQueryPayment(maxQueryPayment);

    final byte[] contents = fileQuery.execute(client);

    HcsIdentityNetwork result = new HcsIdentityNetwork();
    result.network = network;
    result.addressBook = AddressBook.fromJson(new String(contents, Charsets.UTF_8), addressBookFileId);

    return result;
  }

  /**
   * Instantiates existing identity network using a DID generated for this network.
   *
   * @param  client                 The Hedera network client.
   * @param  hcsDid                 The Hedera HCS DID.
   * @param  maxQueryPayment        Maximum HBAR payment for querying the address book file content.
   * @return                        The identity network instance.
   * @throws HederaStatusException  In case querying Hedera File Service fails.
   * @throws HederaNetworkException In case of querying Hedera File Service fails due to transport calls.
   */
  public static HcsIdentityNetwork fromHcsDid(final Client client, final HcsDid hcsDid, final Hbar maxQueryPayment)
      throws HederaNetworkException, HederaStatusException {

    final FileId addressBookFileId = hcsDid.getAddressBookFileId();
    return HcsIdentityNetwork.fromAddressBookFile(client, hcsDid.getNetwork(), addressBookFileId, maxQueryPayment);
  }

  /**
   * Instantiates a {@link HcsDidTransaction} to perform the specified operation on the DID document.
   *
   * @param  operation The operation to be performed on a DID document.
   * @return           The {@link HcsDidTransaction} instance.
   */
  public HcsDidTransaction createDidTransaction(final DidMethodOperation operation) {
    return new HcsDidTransaction(operation, getDidTopicId());
  }

  /**
   * Instantiates a {@link HcsDidTransaction} to perform the specified operation on the DID document.
   *
   * @param  message The DID topic message ready to for sending.
   * @return         The {@link HcsDidTransaction} instance.
   */
  public HcsDidTransaction createDidTransaction(final MessageEnvelope<HcsDidMessage> message) {
    return new HcsDidTransaction(message, getDidTopicId());
  }

  /**
   * Instantiates a {@link HcsVcTransaction} to perform the specified operation on the VC document.
   *
   * @param  operation       The type of operation.
   * @param  credentialHash  Credential hash.
   * @param  signerPublicKey Public key of the signer (issuer).
   * @return                 The transaction instance.
   */
  public HcsVcTransaction createVcTransaction(final HcsVcOperation operation, final String credentialHash,
      final Ed25519PublicKey signerPublicKey) {
    return new HcsVcTransaction(getVcTopicId(), operation, credentialHash, signerPublicKey);
  }

  /**
   * Instantiates a {@link HcsVcTransaction} to perform the specified operation on the VC document status.
   *
   * @param  message         The VC topic message ready to for sending.
   * @param  signerPublicKey Public key of the signer (usually issuer).
   * @return                 The {@link HcsVcTransaction} instance.
   */
  public HcsVcTransaction createVcTransaction(final MessageEnvelope<HcsVcMessage> message,
      final Ed25519PublicKey signerPublicKey) {
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
  public HederaNetwork getNetwork() {
    return network;
  }

  /**
   * Generates a new DID and it's root key.
   *
   * @param  withTid Indicates if DID topic ID should be added to the DID as <i>tid</i> parameter.
   * @return         Generated {@link HcsDid} with it's private DID root key.
   */
  public HcsDid generateDid(final boolean withTid) {
    Ed25519PrivateKey privateKey = HcsDid.generateDidRootKey();
    ConsensusTopicId tid = withTid ? getDidTopicId() : null;

    return new HcsDid(getNetwork(), privateKey, addressBook.getFileId(), tid);
  }

  /**
   * Generates a new DID from the given public DID root key.
   *
   * @param  publicKey A DID root key.
   * @param  withTid   Indicates if DID topic ID should be added to the DID as <i>tid</i> parameter.
   * @return           A newly generated DID.
   */
  public HcsDid generateDid(final Ed25519PublicKey publicKey, final boolean withTid) {
    ConsensusTopicId tid = withTid ? getDidTopicId() : null;
    return new HcsDid(getNetwork(), publicKey, getAddressBook().getFileId(), tid);
  }

  /**
   * Generates a new DID and it's root key using a cryptographically strong random number generator.
   *
   * @param  secureRandom Cryptographically strong random number generator.
   * @param  withTid      Indicates if DID topic ID should be added to the DID as <i>tid</i> parameter.
   * @return              Generated {@link HcsDid} with it's private DID root key.
   */
  public HcsDid generateDid(final SecureRandom secureRandom, final boolean withTid) {
    Ed25519PrivateKey privateKey = HcsDid.generateDidRootKey(secureRandom);
    ConsensusTopicId tid = withTid ? getDidTopicId() : null;

    return new HcsDid(getNetwork(), privateKey, getAddressBook().getFileId(), tid);
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
  public ConsensusTopicId getDidTopicId() {
    return ConsensusTopicId.fromString(addressBook.getDidTopicId());
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
  public ConsensusTopicId getVcTopicId() {
    return ConsensusTopicId.fromString(addressBook.getVcTopicId());
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
   * @param  publicKeysProvider Provider of a public keys acceptable for a given VC hash.
   * @return                    The VC status resolver for this network.
   */
  public HcsVcStatusResolver getVcStatusResolver(
      final Function<String, Collection<Ed25519PublicKey>> publicKeysProvider) {
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
   * @param  publicKeysProvider Provider of a public keys acceptable for a given VC hash.
   * @return                    The VC topic listener.
   */
  public HcsVcTopicListener getVcTopicListener(
      final Function<String, Collection<Ed25519PublicKey>> publicKeysProvider) {
    return new HcsVcTopicListener(getVcTopicId(), publicKeysProvider);
  }
}
