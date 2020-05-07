# Verifiable Credentials Registry - User Guide

---

- [Verifiable Credentials Registry - User Guide](#verifiable-credentials-registry---user-guide)
  - [Credential Schema](#credential-schema)
  - [Creation of Verifiable Credential Documents](#creation-of-verifiable-credential-documents)
  - [Issuance and Revocation](#issuance-and-revocation)
  - [Credential Status Verification](#credential-status-verification)
  - [Continuous Listening to VC Topic Messages](#continuous-listening-to-vc-topic-messages)

---

## Credential Schema

Credential schemas define the attributes of claims issued to credential owners. These may be external JSON schema files stored on a publicly available server, within Hedera File Service, exposed by appnet services or even embedded within Verifiable Credential documents. Appnet implementation may choose any of those options and SDK does not impose any method. Claims in a structure of credential schema are stored within `credentialSubject` property of a VC document and must have an ID property that points to Credential owner's DID. Java classes that represent the credential schema claims shall be implemented as JSON-serializable classes extending `CredentialSubject` class.

For example:

```java
class ExampleCredential extends CredentialSubject {
  @Expose
  private String firstName;
  @Expose
  private String lastName;
  @Expose
  private int age;

  public ExampleCredential(String ownerDid, String firstName, String lastName, int age) {
    this.id = ownerDid;
    this.firstName = firstName;
    this.lastName = lastName;
    this.age = age;
  }

  ...
}
```

## Creation of Verifiable Credential Documents

Appnets may choose to create verifiable credential document using any method of their choice (e.g. a 3rd party RDF, or JSON-LD library).
It is only important that the document is a valid JSON-LD verifiable credential document as specified by W3C here.
The SDK can parse the document and considers only its basic, mandatory fields in order to calculate the credential hash that is used by the identity network. These attributes are:

- `id` - even though VC document specification does not specify it as mandatory, it is on Hedera VC registry and every VC should define one.
- `type` - a list of verifiable credential type values.
- `issuer` - the DID and (optionally) a name of the issuer.
- `issuanceDate` - the date when credential was issued.

The document instance can be obtained from a JSON-LD document by using `fromJson` method:

```java
HcsVcDocumentBase<ExampleCredential> vcFromJson = HcsVcDocumentBase.fromJson(jsonDocument, ExampleCredential.class);
```

The document can also be implemented as a dedicated Java class that extends `HcsVcDocumentBase` class, e.g.:

```java
class ExampleVerifiableCredentialDocument extends HcsVcDocumentBase<ExampleCredential> {

  @Expose
  private String customProperty;

  public String getCustomProperty() {
    return customProperty;
  }

  public void setCustomProperty(String customProperty) {
    this.customProperty = customProperty;
  }
}

```

Once the document has been created, we can calculate its hash by calling `toCredentialHash` method. Verifiable credentials registry on Hedera operates on these hash values as unique identifiers of VC documents.

```java
HcsVcDocumentBase<ExampleCredential> credentialDocument = ...

final String credentialHash = credentialDocument.toCredentialHash();
```

## Issuance and Revocation

Issuers of verifiable credentials create VC documents and issue them to credential subjects via their specific channels. These operations are off-chain and not part of this specification. VC documents usually contain Personally Identifiable Information (PII) and shall be handled securely, exclusively between the involved parties. The only mandatory action issuers must perform on Hedera is registration of the fact that the verifiable credential has been issued (or suspended, resumed or revoked).

Registration of verifiable credential status for a given credential hash is executed in the same way for all event types by using `HcsVcTransaction`. The transaction is created from an instance of `HcsIdentityNetwork` by calling `createVcTransaction` method and specifying the appropriate operation `HcsVcOperation`. Valid operations are:

- ISSUE
- SUSPEND
- RESUME
- REVOKE

Then `HcsVcMessage` must be signed with a DID root key of the subject changing credential status (usually the issuer). Finally `ConsensusMessageSubmitTransaction` must be configured accordingly to Hedera SDK, built and signed.

Here is example issuance code:

```java
// Issuer's #did-root-key
Ed25519PrivateKey privateKey = ...;
HcsIdentityNetwork identityNetwork = ...;
HcsVcDocumentBase<ExampleCredential> credentialDocument = ...;

// Define the operation on a VC document
HcsVcOperation operation = HcsVcOperation.ISSUE;

// Build and execute transaction
TransactionId txId = identityNetwork.createVcTransaction(operation, credentialDocument.toCredentialHash(), privateKey.publicKey)
    // Sign the VC operation message with issuer's key
    .signMessage(doc -> privateKey.sign(doc))
    // Configure and sign HCS topic transaction
    .buildAndSignTransaction(tx -> tx.setMaxTransactionFee(FEE).build(client))
    // Optional: wait until consensus has been reached and get the message back from mirror node
    .onMessageConfirmed(msg -> System.out.println("Verifiable credential " + msg.open().getCredentialHash() + " has been issued"))
    // Handle errors
    .onError(err -> err.printStackTrace())
    // Execute transaction
    .execute(client, mirrorClient);
```

## Credential Status Verification

Once verifiers have validated credential document proofs and claim values off-chain, they shall check if these credentials have not been revoked or suspended in the identity network's credentials registry.
Typically, this status resolution shall be executed against the appnet REST API service as specified in [Verifiable Credentials Registry](/docs/vc-specification.md) specification. In this model, the nodes of the appnet listen to the appropriate VC topic at a mirror node and store the credential hash status in it's dedicated storage. Those parties seeking to resolve a VC status will query an appnet node in order to retrieve the corresponding last valid VC status message. This model may presume a degree of trust between the parties requesting the VC status and the appnet node.

Verifiers who have direct access to a Hedera mirror node and do not want to use appnet REST API service can run VC status resolution query directly against the VC topic on the mirror node. This method may not be recommended as it has to process all messages in the topic from the beginning of its time, but if time is not an issue it can be used for single resolution executions. `HcsVCResolver` can be obtained from the `HcsIdentityNetwork` via `getVcStatusResolver` method. It can accept multiple credential hashes for resolution and when finished will return a map of those hashes strings and their corresponding last valid message posted to the VC topic.

```java
HcsIdentityNetwork identityNetwork = ...;
String credentialHash = ...;

identityNetwork.getVcStatusResolver()
    .addCredentialHash(credentialHash)
    .setTimeout(Duration.ofSeconds(30).toMillis())
    .onError(err -> err.printStackTrace())
    .whenFinished(
        map -> {
          MessageEnvelope<HcsVcMessage> lastValidMessage = map.get(credentialHash);
          if (lastValidMessage == null) {
            System.out.println("Verifiable credential " + credentialHash + "has never been issued on this network.");
          } else {
            System.out.println(credentialHash + " status is: " + lastValidMessage.open().getOperation());
          }
        })
    .execute(mirrorClient);
```

After the last message is received from the topic, the resolver will wait for a given period of time (by default 30 seconds) to wait for more messages. If at this time no more messages arrive, the resolution is considered completed. The waiting time can be modified with `setTimeout` method.

Appnets and direct verifiers can decide which credential status change messages they accept as valid. Some may accept messages signed only by credential issuers, but others may also accept credential suspension or revocation by other parties or credential owners themselves.
This can be configured while obtaining `HcsVCResolver` from the identity network. `getVcStatusResolver` can take as a parameter a provider function that shall return a set of public keys of subject's who's signatures are acceptable for a given credential hash. Other signatures will be rejected by the resolver.

## Continuous Listening to VC Topic Messages

In order for appnets to listen to their VC topic at a mirror node and store credential hash statuses, they may use the SDK's dedicated `MessageListener<HcsVcMessage>` rather than subscribing to the topic via Hedera SDK `MirrorConsensusTopicQuery`. This wrapper verifies incoming messages and parses them to `HcsVcMessage` type automatically.

Here is an example code demonstrating how to obtain the listener instance and subscribe to the VC topic:

```java
// Define the time from which to retrieve topic messages.
// Usually the consensus timestamp of the last message stored by appnet.
Instant startTime = ...;
HcsIdentityNetwork identityNetwork = ...;

MessageListener<HcsVcMessage> vcListener = identityNetwork.getVcTopicListener()
    .setStartTime(startTime)
    // Decide how to handle invalid messages in a topic
    .onInvalidMessageReceived((resp, reason) -> {
      System.out.println("Invalid message received from VC topic: " + reason);
      System.out.println(new String(resp.message, StandardCharsets.UTF_8));
    })
    // Handle errors
    .onError(e -> {
      Code code = null;
      if (e instanceof StatusRuntimeException) {
        code = ((StatusRuntimeException) e).getStatus().getCode();
      }

      if (Code.UNAVAILABLE.equals(code)) {
        // Restart listener if it crashed or lost connection to the mirror node.
        ...
      } else {
        // Handle other errors
        System.err.println("Error while processing message from VC topic: ");
        e.printStackTrace();
      }
    })
    // Start listening and decide how to handle valid incoming messages
    .subscribe(mirrorClient, envelope -> {
      // Store message in appnet storage
      ...
    });
```

[w3c-proofs-registry]: https://w3c-ccg.github.io/ld-cryptosuite-registry/