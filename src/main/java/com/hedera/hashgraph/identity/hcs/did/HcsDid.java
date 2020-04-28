package com.hedera.hashgraph.identity.hcs.did;

import com.google.common.base.Splitter;
import com.google.common.hash.Hashing;
import com.hedera.hashgraph.identity.DidDocumentBase;
import com.hedera.hashgraph.identity.DidSyntax;
import com.hedera.hashgraph.identity.DidSyntax.Method;
import com.hedera.hashgraph.identity.DidSyntax.MethodSpecificParameter;
import com.hedera.hashgraph.identity.HederaDid;
import com.hedera.hashgraph.identity.HederaNetwork;
import com.hedera.hashgraph.sdk.consensus.ConsensusTopicId;
import com.hedera.hashgraph.sdk.crypto.ed25519.Ed25519PrivateKey;
import com.hedera.hashgraph.sdk.crypto.ed25519.Ed25519PublicKey;
import com.hedera.hashgraph.sdk.file.FileId;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.bitcoinj.core.Base58;

/**
 * Hedera Decentralized Identifier for Hedera DID Method specification based on HCS.
 */
public class HcsDid implements HederaDid {
  public static final Method DID_METHOD = Method.HEDERA_HCS;
  private static final int DID_PARAMETER_VALUE_PARTS = 2;

  private ConsensusTopicId didTopicId;
  private FileId addressBookFileId;
  private HederaNetwork network;
  private String idString;
  private String did;
  private Ed25519PublicKey didRootKey;
  private Ed25519PrivateKey privateDidRootKey;

  /**
   * Converts a Hedera DID string into {@link HcsDid} object.
   *
   * @param  didString A Hedera DID string.
   * @return           {@link HcsDid} object derived from the given Hedera DID string.
   */
  public static HcsDid fromString(final String didString) {
    if (didString == null) {
      throw new IllegalArgumentException("DID string cannot be null");
    }

    // Split the DID string by parameter separator.
    // There should be at least one as address book parameter is mandatory by DID specification.
    Iterator<String> mainParts = Splitter.on(DidSyntax.DID_PARAMETER_SEPARATOR).split(didString).iterator();

    ConsensusTopicId topicId = null;
    FileId addressBookFileId = null;

    try {
      Iterator<String> didParts = Splitter.on(DidSyntax.DID_METHOD_SEPARATOR).split(mainParts.next()).iterator();

      if (!DidSyntax.DID_PREFIX.equals(didParts.next())) {
        throw new IllegalArgumentException("DID string is invalid: invalid prefix.");
      }

      String methodName = didParts.next();
      if (!Method.HEDERA_HCS.toString().equals(methodName)) {
        throw new IllegalArgumentException("DID string is invalid: invalid method name: " + methodName);
      }

      String networkName = didParts.next();
      // Extract method-specific parameters: address book file ID and (if provided) DID topic ID.
      Map<String, String> params = extractParameters(mainParts, methodName, networkName);
      addressBookFileId = FileId.fromString(params.get(MethodSpecificParameter.ADDRESS_BOOK_FILE_ID));
      if (params.containsKey(MethodSpecificParameter.DID_TOPIC_ID)) {
        topicId = ConsensusTopicId.fromString(params.get(MethodSpecificParameter.DID_TOPIC_ID));
      }

      String didIdString = didParts.next();
      if (didIdString.length() < 32 || didParts.hasNext()) {
        throw new IllegalArgumentException("DID string is invalid.");
      }

      HederaNetwork didNetwork = HederaNetwork.get(networkName);
      return new HcsDid(didNetwork, didIdString, addressBookFileId, topicId);
    } catch (NoSuchElementException e) {
      throw new IllegalArgumentException("DID string is invalid.", e);
    }
  }

  /**
   * Extracts method-specific URL parameters.
   *
   * @param  mainParts   Iterator over main parts of the DID.
   * @param  methodName  The method name.
   * @param  networkName The network name.
   * @return             A map of method-specific URL parameters and their values.
   */
  private static Map<String, String> extractParameters(final Iterator<String> mainParts,
      final String methodName, final String networkName) {

    Map<String, String> result = new HashMap<>();

    String fidParamName = String.join(DidSyntax.DID_METHOD_SEPARATOR, methodName, networkName,
        MethodSpecificParameter.ADDRESS_BOOK_FILE_ID);
    String tidParamName = String.join(DidSyntax.DID_METHOD_SEPARATOR, methodName, networkName,
        MethodSpecificParameter.DID_TOPIC_ID);

    while (mainParts.hasNext()) {
      String[] paramValue = mainParts.next().split(DidSyntax.DID_PARAMETER_VALUE_SEPARATOR);
      if (paramValue.length != DID_PARAMETER_VALUE_PARTS) {
        continue;
      } else if (fidParamName.equals(paramValue[0])) {
        result.put(MethodSpecificParameter.ADDRESS_BOOK_FILE_ID, paramValue[1]);
      } else if (tidParamName.equals(paramValue[0])) {
        result.put(MethodSpecificParameter.DID_TOPIC_ID, paramValue[1]);
      }
    }

    // Address book is mandatory
    if (!result.containsKey(MethodSpecificParameter.ADDRESS_BOOK_FILE_ID)) {
      throw new IllegalArgumentException("DID string is invalid. Required method-specific URL parameter not found: "
          + MethodSpecificParameter.ADDRESS_BOOK_FILE_ID);
    }

    return result;
  }

  /**
   * Generates a random DID root key.
   *
   * @return A private key of generated public DID root key.
   */
  public static Ed25519PrivateKey generateDidRootKey() {
    return Ed25519PrivateKey.generate();
  }

  /**
   * Generates a random DID root key.
   *
   * @param  secureRandom A cryptographically strong random number generator (RNG).
   * @return              A private key of generated public DID root key.
   */
  public static Ed25519PrivateKey generateDidRootKey(final SecureRandom secureRandom) {
    return Ed25519PrivateKey.generate(secureRandom);
  }

  /**
   * Creates a DID instance.
   *
   * @param network           The Hedera DID network.
   * @param didRootKey        The public key from which DID is derived.
   * @param addressBookFileId The appent's address book {@link FileId}
   * @param didTopicId        The appnet's DID topic ID.
   */
  public HcsDid(final HederaNetwork network, final Ed25519PublicKey didRootKey, final FileId addressBookFileId,
      final ConsensusTopicId didTopicId) {
    this.didTopicId = didTopicId;
    this.addressBookFileId = addressBookFileId;
    this.network = network;
    this.didRootKey = didRootKey;
    this.idString = HcsDid.publicKeyToIdString(didRootKey);
    this.did = buildDid();
  }

  /**
   * Creates a DID instance with private DID root key.
   *
   * @param network           The Hedera DID network.
   * @param privateDidRootKey The private DID root key.
   * @param addressBookFileId The appent's address book {@link FileId}
   * @param didTopicId        The appnet's DID topic ID.
   */
  public HcsDid(final HederaNetwork network, final Ed25519PrivateKey privateDidRootKey, final FileId addressBookFileId,
      final ConsensusTopicId didTopicId) {
    this(network, privateDidRootKey.publicKey, addressBookFileId, didTopicId);
    this.privateDidRootKey = privateDidRootKey;
  }

  /**
   * Creates a DID instance without topic ID specification.
   *
   * @param network           The Hedera DID network.
   * @param didRootKey        The public key from which DID is derived.
   * @param addressBookFileId The appent's address book {@link FileId}
   */
  public HcsDid(final HederaNetwork network, final Ed25519PublicKey didRootKey, final FileId addressBookFileId) {
    this(network, didRootKey, addressBookFileId, null);
  }

  /**
   * Creates a DID instance.
   *
   * @param network           The Hedera DID network.
   * @param idString          The id-string of a DID.
   * @param addressBookFileId The appent's address book {@link FileId}
   * @param didTopicId        The appnet's DID topic ID.
   */
  public HcsDid(final HederaNetwork network, final String idString, final FileId addressBookFileId,
      final ConsensusTopicId didTopicId) {
    this.didTopicId = didTopicId;
    this.addressBookFileId = addressBookFileId;
    this.network = network;

    this.idString = idString;
    this.did = buildDid();
  }

  @Override
  public String toDid() {
    return toString();
  }

  @Override
  public DidDocumentBase generateDidDocument() {
    DidDocumentBase result = new DidDocumentBase(this.toDid());
    if (didRootKey != null) {
      HcsDidRootKey rootKey = HcsDidRootKey.fromHcsIdentity(this, didRootKey);
      result.setDidRootKey(rootKey);
    }

    return result;
  }

  /**
   * Generates DID document base from the given DID and its root key.
   *
   * @param  didRootKey               Public key used to build this DID.
   * @return                          The DID document base.
   * @throws IllegalArgumentException In case given DID root key does not match this DID.
   */
  public DidDocumentBase generateDidDocument(final Ed25519PublicKey didRootKey) {
    DidDocumentBase result = new DidDocumentBase(this.toDid());

    if (didRootKey != null) {
      HcsDidRootKey rootKey = HcsDidRootKey.fromHcsIdentity(this, didRootKey);
      result.setDidRootKey(rootKey);
    }

    return result;
  }

  @Override
  public HederaNetwork getNetwork() {
    return network;
  }

  @Override
  public Method getMethod() {
    return Method.HEDERA_HCS;
  }

  @Override
  public String toString() {
    return did;
  }

  public ConsensusTopicId getDidTopicId() {
    return didTopicId;
  }

  public FileId getAddressBookFileId() {
    return addressBookFileId;
  }

  public String getIdString() {
    return idString;
  }

  /**
   * Constructs an id-string of a DID from a given public key.
   *
   * @param  didRootKey Public Key from which the DID is created.
   * @return            The id-string of a DID that is a Base58-encoded SHA-256 hash of a given public key.
   */
  public static String publicKeyToIdString(final Ed25519PublicKey didRootKey) {
    return Base58.encode(Hashing.sha256().hashBytes(didRootKey.toBytes()).asBytes());
  }

  /**
   * Constructs DID string from the instance of DID object.
   *
   * @return A DID string.
   */
  private String buildDid() {
    String methodNetwork = String.join(DidSyntax.DID_METHOD_SEPARATOR, getMethod().toString(), network.toString());

    StringBuilder sb = new StringBuilder()
        .append(DidSyntax.DID_PREFIX)
        .append(DidSyntax.DID_METHOD_SEPARATOR)
        .append(methodNetwork)
        .append(DidSyntax.DID_METHOD_SEPARATOR)
        .append(idString)
        .append(DidSyntax.DID_PARAMETER_SEPARATOR)
        .append(methodNetwork)
        .append(DidSyntax.DID_METHOD_SEPARATOR)
        .append(MethodSpecificParameter.ADDRESS_BOOK_FILE_ID)
        .append(DidSyntax.DID_PARAMETER_VALUE_SEPARATOR)
        .append(addressBookFileId.toString());

    if (didTopicId != null) {
      sb.append(DidSyntax.DID_PARAMETER_SEPARATOR)
          .append(methodNetwork)
          .append(DidSyntax.DID_METHOD_SEPARATOR)
          .append(MethodSpecificParameter.DID_TOPIC_ID)
          .append(DidSyntax.DID_PARAMETER_VALUE_SEPARATOR)
          .append(didTopicId.toString());
    }

    return sb.toString();
  }

  /**
   * Returns a private key of DID root key.
   * This is only available if it was provided during {@link HcsDid} construction.
   *
   * @return The private key of DID root key.
   */
  public Optional<Ed25519PrivateKey> getPrivateDidRootKey() {
    return Optional.ofNullable(privateDidRootKey);
  }
}
