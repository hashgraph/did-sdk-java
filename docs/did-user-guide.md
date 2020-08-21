# Decentralized Identifiers - User Guide

---

- [Decentralized Identifiers - User Guide](#decentralized-identifiers---user-guide)
  - [DID Generation](#did-generation)
  - [CRUD Methods for DID Document](#crud-methods-for-did-document)
    - [Create, Update, Delete](#create-update-delete)
    - [Read (Resolve)](#read-resolve)
  - [Continuous Listening to DID Topic Messages](#continuous-listening-to-did-topic-messages)
  
---

## DID Generation

A DID is represented in SDK as `HcsDid` object and can be easily converted to it's DID string form by calling its `toDid()` or `toString()` method. New decentralized identifiers can be generated in multiple handy ways:

- from already instantiated network:

```java
HcsIdentityNetwork identityNetwork = ...;

// From a given DID root key:
Ed25519PrivateKey didRootKey = ...;
HcsDid hcsDid = identityNetwork.generateDid(didRootKey.publicKey, false);

// Without having a DID root key - it will be generated automatically:
// Here we decided to add DID topic ID parameter `tid` to the DID.
HcsDid hcsDidWithDidRootKey = identityNetwork.generateDid(true);
Ed25519PrivateKey didRootKeyPrivateKey = hcsDidWithDidRootKey.getPrivateDidRootKey().get();

// Without having a DID root key - it will be generated automatically with secure random generator:
HcsDid hcsDidSRWithDidRootKey = identityNetwork.generateDid(SecureRandom.getInstanceStrong(), false);
Ed25519PrivateKey srDidRootKeyPrivateKey = hcsDidSRWithDidRootKey.getPrivateDidRootKey().get();
```

- or by directly constructing `HcsDid` object:

```java
Ed25519PrivateKey didRootKey = HcsDid.generateDidRootKey();
FileId addressBookFileId = FileId.fromString("<hedera.address-book-file.id>");

HcsDid did = new HcsDid(HederaNetwork.TESTNET, didRootKey.publicKey, addressBookFileId);
```

Please note that generated DIDs are completely off-chain. They are not published to the Hedera network unless specific DID document message is sent to HCS DID topic.

Existing Hedera DID strings can be parsed into `HcsDid` object by calling `fromString` method:

```java
String didString = "did:hedera:testnet:7c38oC4ytrYDGCqsaZ1AXt7ZPQ8etzfwaxoKjfJNzfoc;hedera:testnet:fid=0.0.1";
HcsDid did = HcsDid.fromString(didString);
```

## CRUD Methods for DID Document

A DID document is a graph-based data structure typically expressed using JSON-LD 1.1 format. Its structure can be extended based on DID subject requirements.
As currently there is no standard Java library for DID documents, nor even JSON-LD version 1.1 documents, this SDK uses a custom `DidDocumentBase` class that constructs JSON-LD DID document with only mandatory DID document attributes defined by [W3C DID Specification][w3c-did-core] and those required by [Hedera DID Method][did-method-spec]. Appnet creators can extend `DidDocumentBase` class to include other attributes in the DID document or construct a JSON-LD string in their own way (e.g. using 3rd party RDF libraries).

Having `HcsDid` object, we can generate a DID document for it:

```java
HcsDid did = ...;

DidDocumentBase didDocument = did.generateDidDocument();
String didDocumentJson = didDocument.toJson();

System.out.println(didDocumentJson);
```

This will produce the following document:

```json
{
   "@context":"https://www.w3.org/ns/did/v1",
   "id":"did:hedera:testnet:8D6uYQ3VUTTFk9YnqNAkLDUx9vWbycZLFXwBbbmKY2k7;hedera:testnet:fid=0.0.1",
   "publicKey":[
      {
         "id":"did:hedera:testnet:8D6uYQ3VUTTFk9YnqNAkLDUx9vWbycZLFXwBbbmKY2k7;hedera:testnet:fid=0.0.1#did-root-key",
         "type":"Ed25519VerificationKey2018",
         "controller":"did:hedera:testnet:8D6uYQ3VUTTFk9YnqNAkLDUx9vWbycZLFXwBbbmKY2k7;hedera:testnet:fid=0.0.1",
         "publicKeyBase58":"EmxMNxbVb4AKV4HE2iMjQQzRea2t3ZzHinZU4z7sAC4X"
      }
   ],
   "authentication":[
      "did:hedera:testnet:8D6uYQ3VUTTFk9YnqNAkLDUx9vWbycZLFXwBbbmKY2k7;hedera:testnet:fid=0.0.1#did-root-key"
   ]
}
```

### Create, Update, Delete

C(not R)UD operations on a given DID document are all executed in the same way, by using `HcsDidTransaction`.
The transaction is created from an instance of `HcsIdentityNetwork` by calling `createDidTransaction` method and specifying the appropriate CRUD operation, e.g. `DidMethodOperation.CREATE`. Then `HcsDidTransaction` must be provided with the DID document, which has to be signed by DID root key of the DID subject. Finally `ConsensusMessageSubmitTransaction` must be configured accordingly to Hedera SDK, built and signed.

Here is example DID document creation code:

```java
Client client = ...;
MirrorClient mirrorClient = ...;
HcsIdentityNetwork identityNetwork = ...;

Ed25519PrivateKey didRootKey = ...;
HcsDid hcsDid = ...;

String didDocument = hcsDid.generateDidDocument().toJson();

// Build and execute transaction
identityNetwork.createDidTransaction(DidMethodOperation.CREATE)
    // Provide DID document as JSON string
    .setDidDocument(didDocument)
    // Sign it with DID root key
    .signDidDocument(doc -> didRootKey.sign(doc))
    // Configure ConsensusMessageSubmitTransaction, build it and sign if required by DID topic
    .buildAndSignTransaction(tx -> tx.setMaxTransactionFee(new Hbar(2)).build(client))
    // Define callback function when consensus was reached and DID document came back from mirror node
    .onDidDocumentReceived(msg -> {
      System.out.println("DID document published!");
      System.out.println(msg.getDidDocument());
    })
    // Execute transaction
    .execute(client, mirrorClient);
```

Appnet implementations can optionally add a callback listener and receive an event when the HCS message carrying the DID operation reached consensus and was subsequently propagated to the mirror network.
They can also have their own mirror node listener and catch incoming messages from the relevant DID topic.

Once a Hedera timestamped DID document is received from a mirror node, appnets can store them in their own storage solution in support of future resolution requests.

Here is example code demonstrating the use of `HcsDidTopicListener` to  receive parsed, validated and decrypted messages from a mirror:

```java
HcsIdentityNetwork identityNetwork = ...;
HcsDidTopicListener listener = identityNetwork.getDidTopicListener();

listener.setStartTime(Instant.MIN)
    .setIgnoreInvalidMessages(true)
    .setIgnoreErrors(false)
    .onError(err -> System.err.println(err))
    .subscribe(mirrorClient, msg -> {
        System.out.println("Message received");
        System.out.println(msg.getDidDocument());
        // Store message in appnet's system
        ...
    });
```

The listener can be restarted to process messages at any given `startTime` so that local storage can catch up to the state of the mirror node.

### Read (Resolve)

Typically, DID resolution shall be executed against the appnet REST API service as specified in [Hedera DID Method][did-method-spec]. In this model, the nodes of the appnet listen to the appropriate DID topic at a mirror node and store the DID Documents in it's dedicated storage (as described above). Those parties seeking to resolve a DID will query an appnet node in order to retrieve the corresponding DID Document. This model may presume a degree of trust between the parties requesting the DID Document and the appnet node.

Resolvers who have direct access to a Hedera mirror node and do not want to use appnet REST API service can run DID resolution query directly against the DID topic on the mirror node. This method may not be recommended as it has to process all messages in the topic from the beginning of its time, but if time is not an issue it can be used for single resolution executions. `HcsDidResolver` can be obtained from the `HcsIdentityNetwork` via `getDidResolver` method. It can accept multiple DIDs for resolution and when finished will return a map of DID strings and their corresponding last valid message posted to the DID topic.

```java
HcsIdentityNetwork identityNetwork = ...;
String did = "did:hedera:testnet:7c38oC4ytrYDGCqsaZ1AXt7ZPQ8etzfwaxoKjfJNzfoc;hedera:testnet:fid=0.0.1";
identityNetwork.getDidResolver()
    .addDid(did)
    .whenFinished(results -> {
        MessageEnvelope<HcsDidMessage> envelope = results.get(did);
        if (envelope == null) {
          // DID document not found
          return;
        }

        HcsDidMessage msg = envelope.open();
        if (DidMethodOperation.DELETE.equals(msg.getOperation())) {
          // DID was deleted (revoked)
        } else {
          // Process DID document
          System.out.println(msg.getDidDocument());
        }
    })
    .execute(mirrorClient);
```

After the last message is received from the topic, the resolver will wait for a given period of time (by default 30 seconds) to wait for more messages. If at this time no more messages arrive, the resolution is considered completed. The waiting time can be modified with `setTimeout` method.

## Continuous Listening to DID Topic Messages

In order for appnets to listen to their DID topic at a mirror node and store DID documents, they may use the SDK's dedicated `MessageListener<HcsDidMessage>` rather than subscribing to the topic via Hedera SDK `MirrorConsensusTopicQuery`. This wrapper verifies incoming messages and parses them to `HcsDidMessage` type automatically.

Here is an example code demonstrating how to obtain the listener instance and subscribe to the DID topic:

```java
// Define the time from which to retrieve topic messages.
// Usually the consensus timestamp of the last message stored by appnet.
Instant startTime = ...;
HcsIdentityNetwork identityNetwork = ...;

MessageListener<HcsDidMessage> didListener = identityNetwork.getDidTopicListener()
    .setStartTime(startTime)
    // Decide how to handle invalid messages in a topic
    .onInvalidMessageReceived((resp, reason) -> {
      System.out.println("Invalid message received from DID topic: " + reason);
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
        System.err.println("Error while processing message from DID topic: ");
        e.printStackTrace();
      }
    })
    // Start listening and decide how to handle valid incoming messages
    .subscribe(mirrorClient, envelope -> {
      // Store message in appnet storage
      ...
    });
```

[did-method-spec]: https://github.com/hashgraph/did-method
[w3c-did-core]: https://w3c.github.io/did-core/