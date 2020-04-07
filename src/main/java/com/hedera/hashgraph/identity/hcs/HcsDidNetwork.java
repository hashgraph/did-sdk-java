package com.hedera.hashgraph.identity.hcs;

import com.google.common.base.Charsets;
import com.hedera.hashgraph.identity.DidDocumentOperation;
import com.hedera.hashgraph.identity.HederaNetwork;
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
import java.util.HashMap;
import java.util.Map;

/**
 * Appnet's identity network based on Hedera HCS DID method specification.
 */
public final class HcsDidNetwork {
  /**
   * The address book of appnet's identity network.
   */
  private HcsDidNetworkAddressBook addressBook;

  /**
   * The Hedera network on which this identity network is created.
   */
  private HederaNetwork network;

  /**
   * Creates a new identity network instance.
   */
  private HcsDidNetwork() {
    // This constructor is intentionally empty. Nothing special is needed here.
  }

  /**
   * Instantiates existing identity network from a provided address book.
   *
   * @param  network     The Hedera network.
   * @param  addressBook The {@link HcsDidNetworkAddressBook} of the identity network.
   * @return             The identity network instance.
   */
  public static HcsDidNetwork fromAddressBook(final HederaNetwork network, final HcsDidNetworkAddressBook addressBook) {
    HcsDidNetwork result = new HcsDidNetwork();
    result.network = network;
    result.addressBook = addressBook;

    return result;
  }

  /**
   * Instantiates existing identity network using an address book file read from Hedera File Service.
   *
   * @param  client                 The Hedera network client.
   * @param  network                The Hedera network.
   * @param  addressBookFileId      The FileID of {@link HcsDidNetworkAddressBook} file stored on Hedera File Service.
   * @param  maxQueryPayment        Maximum HBAR payment for querying the address book file content.
   * @return                        The identity network instance.
   * @throws HederaStatusException  In case querying Hedera File Service fails.
   * @throws HederaNetworkException In case of querying Hedera File Service fails due to transport calls.
   */
  public static HcsDidNetwork fromAddressBookFile(final Client client, final HederaNetwork network,
      final FileId addressBookFileId,
      final Hbar maxQueryPayment)
      throws HederaNetworkException, HederaStatusException {

    final FileContentsQuery fileQuery = new FileContentsQuery().setFileId(addressBookFileId);
    fileQuery.setMaxQueryPayment(maxQueryPayment);

    final byte[] contents = fileQuery.execute(client);

    HcsDidNetwork result = new HcsDidNetwork();
    result.network = network;
    result.addressBook = HcsDidNetworkAddressBook.fromJson(new String(contents, Charsets.UTF_8), addressBookFileId);

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
  public static HcsDidNetwork fromHcsDid(final Client client, final HcsDid hcsDid, final Hbar maxQueryPayment)
      throws HederaNetworkException, HederaStatusException {

    final FileId addressBookFileId = hcsDid.getAddressBookFileId();
    return HcsDidNetwork.fromAddressBookFile(client, hcsDid.getNetwork(), addressBookFileId, maxQueryPayment);
  }

  /**
   * Instantiates a {@link HcsDidTransaction} to perform the specified operation on the DID document.
   *
   * @param  operation The operation to be performed on a DID document.
   * @return           The {@link HcsDidTransaction} instance.
   */
  public HcsDidTransaction createDidTransaction(final DidDocumentOperation operation) {
    return new HcsDidTransaction(operation, ConsensusTopicId.fromString(addressBook.getDidTopicId()));
  }

  /**
   * Returns the address book of this identity network.
   *
   * @return The address book of this identity network.
   */
  public HcsDidNetworkAddressBook getAddressBook() {
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
   * @return         A map entry consisting of a private key of DID root key and {@link HcsDid} as a value.
   */
  public Map.Entry<Ed25519PrivateKey, HcsDid> generateDid(final boolean withTid) {
    Ed25519PrivateKey privateKey = HcsDid.generateDidRootKey();
    ConsensusTopicId tid = withTid ? ConsensusTopicId.fromString(addressBook.getDidTopicId()) : null;
    HcsDid hcsDid = new HcsDid(getNetwork(), privateKey.publicKey, addressBook.getFileId(), tid);
    Map<Ed25519PrivateKey, HcsDid> map = new HashMap<>(1);
    map.put(privateKey, hcsDid);

    return map.entrySet().iterator().next();
  }

  /**
   * Generates a new DID from the given public DID root key.
   *
   * @param  publicKey A DID root key.
   * @param  withTid   Indicates if DID topic ID should be added to the DID as <i>tid</i> parameter.
   * @return           A newly generated DID.
   */
  public HcsDid generateDid(final Ed25519PublicKey publicKey, final boolean withTid) {
    ConsensusTopicId tid = withTid ? ConsensusTopicId.fromString(addressBook.getDidTopicId()) : null;
    return new HcsDid(getNetwork(), publicKey, getAddressBook().getFileId(), tid);
  }

  /**
   * Generates a new DID and it's root key using a cryptographically strong random number generator.
   *
   * @param  secureRandom Cryptographically strong random number generator.
   * @param  withTid      Indicates if DID topic ID should be added to the DID as <i>tid</i> parameter.
   * @return              A map entry consisting of a private key of DID root key and {@link HcsDid} as a value.
   */
  public Map.Entry<Ed25519PrivateKey, HcsDid> generateDid(final SecureRandom secureRandom, final boolean withTid) {
    Ed25519PrivateKey privateKey = HcsDid.generateDidRootKey(secureRandom);
    ConsensusTopicId tid = withTid ? ConsensusTopicId.fromString(addressBook.getDidTopicId()) : null;

    HcsDid hcsDid = new HcsDid(getNetwork(), privateKey.publicKey, getAddressBook().getFileId(), tid);
    Map<Ed25519PrivateKey, HcsDid> map = new HashMap<>(1);
    map.put(privateKey, hcsDid);

    return map.entrySet().iterator().next();
  }

  // TODO resolve DID
}
