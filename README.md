
# Hedera™ Hashgraph DID - Java SDK
[![License: Apache-2.0](https://img.shields.io/badge/license-Apache--2.0-green)](LICENSE)
[![Documentation](https://img.shields.io/badge/javadoc-reference-informational)](docs/index.html)

![LINE](https://img.shields.io/badge/line--coverage-84%25-brightgreen.svg)
![INSTRUCTION](https://img.shields.io/badge/instruction--coverage-85%25-brightgreen.svg)
![METHOD](https://img.shields.io/badge/method--coverage-86%25-brightgreen.svg)
![CLASS](https://img.shields.io/badge/class--coverage-97%25-brightgreen.svg)
![COMPLEXITY](https://img.shields.io/badge/complexity-1.95-brightgreen.svg)


This repository contains the Java SDK for managing DID Documents & Verifiable Credentials framework using the Hedera Consensus Service.

This SDK is desiogne to simplify :
- creation of identity networks within appnets,
- generation of decentralized identifiers for [Hedera DID Method](https://github.com/hashgraph/did-method),
- creation, update, deletion and resolution of DID documents in appnet identity networks,
- issuance, verification and revocation of [Verifiable Credentials](https://www.w3.org/TR/vc-data-model/).

## Table of Contents
- [Hedera™ Hashgraph DID - Java SDK](#hedera%e2%84%a2-hashgraph-did---java-sdk)
  - [Table of Contents](#table-of-contents)
  - [Usage](#usage)
    - [Dependency Declaration](#dependency-declaration)
    - [Identity Network Creation](#identity-network-creation)
    - [Existing Identity Network Instantiation](#existing-identity-network-instantiation)
    - [Decentralized Identifiers](#decentralized-identifiers)
      - [DID Generation](#did-generation)
      - [CRUD Methods for DID Document](#crud-methods-for-did-document)
        - [Create, Update, Delete](#create-update-delete)
        - [Read (Resolve)](#read-resolve)
    - [Verifiable Credentials](#verifiable-credentials)
      - [Status Registration](#status-registration)
      - [Status Verification](#status-verification)
      - [Appnet Relay Interface for Verifiable Credentials](#appnet-relay-interface-for-verifiable-credentials)
      - [SDK Usage](#sdk-usage)
        - [Verifiable Credential Document](#verifiable-credential-document)
          - [Credential Schema](#credential-schema)
        - [Credential Hash calculation](#credential-hash-calculation)
        - [Status Registration](#status-registration-1)
        - [Status Verification](#status-verification-1)
  - [License Information](#license-information)
  - [References](#references)

## Usage

### Dependency Declaration

TODO: To be updated after release to MVN Repository

__Maven__

```xml
<dependency>
  <groupId>com.hedera.hashgraph</groupId>
  <artifactId>identity</artifactId>
  <version>1.0.0</version>
</dependency>
```

__Gradle__

```gradle
implementation group: 'com.hedera.hashgraph', name: 'identity', version: '1.0.0'
```

### Identity Network Creation

An identity appnet should have the following artifacts created on the Hedera mainnet:
- [Address book](https://github.com/hashgraph/Identity-did/blob/master/did-method-specification.md#appnet-address-book) for the members of the appnet stored as a file in Hedera File Service
- Hedera Consensus Service topic for DID Document messages. HCS messages creating, updating, or deleting DID Documents are submitted to this topic.
- Hedera Consensus Service topic for Verifiable Credentials messages. HCS messages issuing, suspending, or revoking Verifible Credentials are submitted to this topic.

These could be set up manually by appnet administrators or can be created using `HcsIdentityNetworkBuilder` as follows:
```java
Ed25519PublicKey myPublicKey = ...;
Client client = Client.forTestnet();

HcsIdentityNetwork identityNetwork = new HcsIdentityNetworkBuilder()
    .setNetwork(HederaNetwork.TESTNET)
    .setAppnetName("MyIdentityAppnet")
    .addAppnetDidServer("https://appnet-did-server-url:port/path-to-did-api")
    .buildAndSignAddressBookCreateTransaction(tx -> tx
        .addKey(myPublicKey)
        .setMaxTransactionFee(new Hbar(2))
        .build(client))
    .buildAndSignDidTopicCreateTransaction(tx -> tx
        .setAdminKey(myPublicKey)
        .setMaxTransactionFee(new Hbar(2))
        .setSubmitKey(myPublicKey)
        .setTopicMemo("MyIdentityAppnet DID topic")
        .build(client))
    .buildAndSignVcTopicCreateTransaction(tx -> tx
        .setAdminKey(myPublicKey)
        .setMaxTransactionFee(new Hbar(2))
        .setSubmitKey(myPublicKey)
        .setTopicMemo("MyIdentityAppnet VC topic")
        .build(client))
    .execute(client);
```

`FileCreateTransaction` for address book file creation and `ConsensusTopicCreateTransaction` for DID and VC topic creation can be configured in a standard way as specified in Hedera Java SDK.

### Existing Identity Network Instantiation
Once the above identity network artifacts have been created, appnets will require `HcsIdentityNetwork` instance to interact with identity network.
It can be initialized in multiple ways:
- from an existing address book file stored by appnet - which will not require querying Hedera File Service:
```java
// Read address book JSON from a local file (or another appnet's source)
Path pathToAddressBookFile = Paths.get("<pathToLocalAddressBookJsonFile.json>");
String addressBookJson = new String(Files.readAllBytes(pathToAddressBookFile), StandardCharsets.UTF_8);
FileId addressBookFileId = FileId.fromString("<hedera.file.id>");

AddressBook addressBook = AddressBook.fromJson(addressBookJson, addressBookFileId);

HcsIdentityNetwork identityNetwork = HcsIdentityNetwork.fromAddressBook(HederaNetwork.TESTNET, addressBook);
```

- from address book FileId - which will query Hedera File Service and read the content of the address book file:
```java
Client client = Client.forTestnet();
Hbar maxFileQueryPayment = new Hbar(2);
FileId addressBookFileId = FileId.fromString("<hedera.file.id>");

HcsIdentityNetwork identityNetwork = HcsIdentityNetwork.fromAddressBookFile(client, HederaNetwork.TESTNET, addressBookFileId, maxFileQueryPayment);
```

- from a Hedera DID string that will extract `fid` parameter and query address book file content from Hedera File Service:
```java
// Initialize network from this DID, reading address book file from Hedera File Service
String did = "did:hedera:testnet:7c38oC4ytrYDGCqsaZ1AXt7ZPQ8etzfwaxoKjfJNzfoc;hedera:testnet:fid=0.0.1";
Client client = Client.forTestnet();
Hbar maxFileQueryPayment = new Hbar(2);

HcsIdentityNetwork identityNetwork = HcsIdentityNetwork.fromHcsDid(client, HcsDid.fromString(did), maxFileQueryPayment);
```
### Decentralized Identifiers
Decentralized Identifiers based on Hedera DID Method must conform to its ABNF notation. SDK provides useful utilities that will take care of constructing a valid DID string for each network.

#### DID Generation
A DID is represented in SDK as `HcsDid` object and can be easily converted to it's DID string form by calling its `toDid()` or `toString()` method. New decentralized identifiers can be generated in multiple handly ways:
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

Please note that generated DIDs are completely offchain. They are not published to the Hedera network unless specific DID document message is sent to HCS DID topic.

Existing Hedera DID strings can be parsed into `HcsDid` object by calling `fromString` method:
```java
String didString = "did:hedera:testnet:7c38oC4ytrYDGCqsaZ1AXt7ZPQ8etzfwaxoKjfJNzfoc;hedera:testnet:fid=0.0.1";
HcsDid did = HcsDid.fromString(didString);
```

#### CRUD Methods for DID Document
A DID document is a graph-based data structure typically expressed using JSON-LD 1.1 format. Its structure can be extended based on DID subject requirements.
As currently there is no standard Java library for DID documents, nor even JSON-LD version 1.1 documents, this SDK uses a custom `DidDocumentBase` class that constructs JSON-LD DID document with only mandatory DID document attributes defined by [W3C DID Specification](https://w3c.github.io/did-core/) and those required by [Hedera DID Method](https://github.com/hashgraph/did-method). Appnet creators can extend `DidDocumentBase` class to include other attributes in the DID document or construct a JSON-LD string in their own way (e.g. using 3rd party RDF libraries).

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

##### Create, Update, Delete
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

##### Read (Resolve)
Typically, DID resolution shall be executed against the appnet's REST API service as specified in [Hedera DID Method](https://github.com/hashgraph/identity-did). In this model, the nodes of the appnet listen to the appropriate DID topic at a mirror node and store the DID Documents in it's dedicated storage (as described above). Those parties seeking to resolve a DID will query an appnet node in order to retrieve the corresponding DID Document. This model may presume a degree of trust between the parties requesting the DID DOcument and the appnet node.  

Resolvers who have direct access to a Hedera mirror node and do not want to use appnet's REST API service can run DID resolution query directy against the DID topic on the mirror node. This method may not be recommended as it has to process all messages in the topic from the beginning of its time, but if time is not an issue it can be used for single resolution executions. `HcsDidResolver` can be obtained from the `HcsIdentityNetwork` via `getResolver` method. It can accept multiple DIDs for resolution and when finished will return a map of DID strings and their corresponding last valid message posted to the DID topic.

```java
HcsIdentityNetwork identityNetwork = ...;
String did = "did:hedera:testnet:7c38oC4ytrYDGCqsaZ1AXt7ZPQ8etzfwaxoKjfJNzfoc;hedera:testnet:fid=0.0.1";
identityNetwork.getResolver()
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

### Verifiable Credentials
Besides supporting the management of DID Documents are per the HCS DID method specification, the Hedera DID Java SDK provides a verifiable credentials registry framwork. Issuers and verifiers working with appnets can utilize it to register verifiable credential events such as: issuance, revication, suspension or resumption of claims. The model is very similar to how HCS messages are used to manage the lifecycle of DID DOcuments. However, no verifiable credential themselves are ever submitted over the Hedera network. Instead, only hashes of such credentials are messaged over HCS and and so timestamped and ordered. As in the DID method specification, verifiable credentials have a dedicated Consensus Service Topic within the appnet to which issuers send messages to register verifiable credential events.

#### Status Registration
A valid Issuance, Revocation, Susspension or Resumption,message must have a JSON structure defined by a [vc-message-schema](vc-message.schema.json) and contains the following properties:
- `mode` - Describes the mode in which the message content is provided. Valid values are: `plain` or `encrypted`. Messages in `encrypted` mode have `credentialHash` credentialHashattribute encrypted separately. Other attributes are plain.
- `message` - The message content with the following attributes:
  - `operation` - Operation on a verifiable credential.  Valid values are: `issue`, `suspend`, `resume` and `revoke`.
  - `credentialHash` -  - This field may contain either:
    - a hash of a verifiable credential document as Base64-encoded string,
    - or a Base64-encoded encrypted representation of this hash, where the encryption and decryption methods and keys are defined by appnet owners.
  - `timestamp` - A message creation time. The value MUST be a valid XML datetime value, as defined in section 3.3.7 of [W3C XML Schema Definition Language (XSD) 1.1 Part 2: Datatypes](https://www.w3.org/TR/xmlschema11-2/). This datetime value MUST be normalized to UTC 00:00, as indicated by the trailing "Z". It is important to note that this timestamp is a system timestamp as a variable part of the message and does not represent a consensus timestamp of a message submitted to the DID topic.
- `signature` - A Base64-encoded signature that is a result of signing a minified JSON string of a message attribute with a private key corresponding to the public key `#did-root-key` in the DID document of the credential issuer. Appnets and verifiers may decide to accept revocation messages signed by credential owners. The framework does not impose any limitation on how message signatures are verified.

Neither the Hedera network nor mirror nodes validate the credential documents, their hashes or message signatures. It is appnets that, as part of their subscription logic, must validate messages based on the above criteria. The messages with duplicate signatures shall be disregarded and only the first one with verified signature shall be considered valid (in consensus timestamp order).

Here is an example of a complete message wrapped in an envelope and signed:
```json
{
   "mode":"plain",
   "message":{
      "operation":"issue",
      "credentialHash":"De6CHEKt37Q91oHPhNQYxzUySq3HHb7d5cUZft8uBiow",
      "timestamp":"2020-04-28T11:11:16.449Z"
   },
   "signature":"IQ+eX33sslafu7eeaMrAswQOO33jl5xw5mfgwB8GnIEbsr/631JADGrGLymfpZEbO7RgVjGKRZIohAeDZukKAA=="
}
```

It is a responsibility of an appnet's administrators to decide who can submit messages to their VC topic. Access control of message submission is defined by a `submitKey` property of `ConsensusCreateTopicTransaction` body. If no `submitKey` is defined for a topic, then any party can submit messages against the topic. Detailed information on Hedera Consensus Service APIs can be found in the official [Hedera API documentation](https://docs.hedera.com/hedera-api/consensus/consensusservice).


#### Status Verification
Verifiers upon accepting a claim from credential subjects shall establish the following:
1. trust to the issuing subjects (e.g. by resolving a known or trusted DID of the issuer and following its authentication procedure)
2. validation of the credential proofs - according to the proof type defined by the issuer
3. verification of the credential status in Hedera credential registry
4. validation of claims against verifier's requirements

Due to the open nature of verifiable credential documents and multiple ways of presentation of proofs inside them, validation of credential proofs is at the responsibility of the verifier. Appnets may support verifiers providing functionalities to automate this process within the requirements of their network.
Hedera SDK for Verifiable Credentials provides only mechanisms to verify the status of a credential to determine if it was not suspended or revoked.
An example implementation of an appnet is available within the SDK's github repository.

#### Appnet Relay Interface for Verifiable Credentials
TODO: to be documented when example application is implemented.

#### SDK Usage
TODO: subject to change - work in progress

At this point it is assumed that Hedera Identity Netowrk has already been set up, issuers and credential owners have registered and exchanged their DIDs.
##### Verifiable Credential Document Construction

###### Credential Schema
Credential schemas define the attributes of claims issued to credential owners. These may be external JSON schema files stored on a publicly available server, within Hedera File Service, exposed by appnet services or even embedded within Verifiable Credential documents. Appnet implementation may choose any of those options and SDK is not imposing any limitation in this space. Claims in a structure of credential schema are stored within `credentialSubject` property of a VC document and must have an ID property that points to Credential owner's DID. Java classes that represent the credential schema claims shall be implemented as JSON-serializable classes extending `CredentialSubject` class.

For example:
```java
class ExampleCredential extends CredentialSubject {
  @Expose
  private String firstName;
  @Expose
  private String lastName;
  @Expose
  private int age;

  public DemoAccessCredential(String ownerDid, String firstName, String lastName, int age) {
    this.id = ownerDid;
    this.firstName = firstName;
    this.lastName = lastName;
    this.age = age;
  }

  ...
}
```
##### Credential Hash calculation
Hedera VC registry operates on credential hashes that are derived from verifiable credential documents by calculating a hash of a subset of this document attributes that are constant and mandatory. In order for verifiers to be able to resolve the status of a credential they must be able to calculate its hash without having access to all credential subjects or claim attributes in the document (in case they were shared with them partially or using ZKPs). For this reason the credential hash is defined as a Base64-encoded SHA-256 hash of a minimized JSON document consisting of the following attributes of the original VC document:

- `id` - even though VC document specification does not specify it as mandatory, it is on Hedera VC registry and every VC should define one.
- `type` - a list of verifiable credential type values.
- `issuer` - the DID and (optionally) a name of the issuer.
- `issuanceDate` - the date when credential was issued.

Having a verifiable credential document in a form of a JSON file we can calculate its credential hash using `toCredentialHash` method of `HcsVcDocumentBase` class:
```java
HcsVcDocumentBase<DemoAccessCredential> vcFromJson = HcsVcDocumentBase.fromJson(json, ExampleCredential.class);
String credentialHash = vcFromJson.toCredentialHash();
```

##### Status Registration


##### Status Verification


## License Information
Licensed under Apache License, Version 2.0 – see [LICENSE](LICENSE) in this repo or on the official Apache page  <http://www.apache.org/licenses/LICENSE-2.0>

## References
* <https://github.com/hashgraph/did-method>
* <https://github.com/hashgraph/hedera-sdk-java>
* <https://docs.hedera.com/hedera-api/>
* <https://www.hedera.com/>
* <https://w3c-ccg.github.io/did-spec/>
* <https://www.w3.org/TR/vc-data-model/>
