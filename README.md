
# Hedera™ Hashgraph DID - Java SDK
[![License: Apache-2.0](https://img.shields.io/badge/license-Apache--2.0-green)](LICENSE)
[![Documentation](https://img.shields.io/badge/javadoc-reference-informational)](docs/index.html)

![LINE](https://img.shields.io/badge/line--coverage-86%25-brightgreen.svg)
![BRANCH](https://img.shields.io/badge/branch--coverage-63%25-yellow.svg)
![COMPLEXITY](https://img.shields.io/badge/complexity-1.91-brightgreen.svg)
![INSTRUCTION](https://img.shields.io/badge/instruction--coverage-86%25-brightgreen.svg)
![METHOD](https://img.shields.io/badge/method--coverage-84%25-brightgreen.svg)
![CLASS](https://img.shields.io/badge/class--coverage-83%25-brightgreen.svg)


This repository contains Java SDK for Hedera Hashgraph DID framework based on the draft version of [DID Method Specification](https://github.com/hashgraph/identity-did) on top of Hedera Consensus Service.

The goal of this SDK is to simplify :
- creation of identity networks witnin appnets, 
- generation of decentralized identifiers for [Hedera DID Method](https://github.com/hashgraph/identity-did), 
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
      - [Issuing](#issuing)
      - [Revocation](#revocation)
      - [Verification](#verification)
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
Appnets that want to use Hedera DID Method shall create the following artifacts for their identity network:
- [Address book](https://github.com/hashgraph/Identity-did/blob/master/did-method-specification.md#appnet-address-book) file in Hedera File Service
- Hedera Consensus Service topic for DID Document messages
- Hedera Consensus Service topic for Verifiable Credentials messages

These could be set up manually by appnet administrators or can be created using `HcsDidNetworkBuilder` as follows:
```java
Ed25519PublicKey myPublicKey = ...;
Client client = Client.forTestnet();

HcsDidNetwork didNetwork = new HcsDidNetworkBuilder()
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
Once identity network artifacts have been created, appnets will require `HcsDidNetwork` instance to interact with identity network.
It can be initialized in multiple ways:
- from existing address book file stored by appnet - which will not require querying Hedera File Service:
```java
// Read address book JSON from a local file (or another appnet's source)
Path pathToAddressBookFile = Paths.get("<pathToLocalAddressBookJsonFile.json>");
String addressBookJson = new String(Files.readAllBytes(pathToAddressBookFile), StandardCharsets.UTF_8);
FileId addressBookFileId = FileId.fromString("<hedera.file.id>");

HcsDidNetworkAddressBook addressBook = HcsDidNetworkAddressBook.fromJson(addressBookJson, addressBookFileId);

HcsDidNetwork didNetwork = HcsDidNetwork.fromAddressBook(HederaNetwork.TESTNET, addressBook);
```

- from address book FileId - which will query Hedera File Service and read the content of the address book file:
```java
Client client = Client.forTestnet();
Hbar maxFileQueryPayment = new Hbar(2);
FileId addressBookFileId = FileId.fromString("<hedera.file.id>");

HcsDidNetwork didNetwork = HcsDidNetwork.fromAddressBookFile(client, HederaNetwork.TESTNET, addressBookFileId, maxFileQueryPayment);
```

- from a Hedera DID string that will extract `fid` parameter and query address book file content from Hedera File Service:
```java
// Initialize network from this DID, reading address book file from Hedera File Service
String did = "did:hedera:testnet:7c38oC4ytrYDGCqsaZ1AXt7ZPQ8etzfwaxoKjfJNzfoc;hedera:testnet:fid=0.0.1";
Client client = Client.forTestnet();
Hbar maxFileQueryPayment = new Hbar(2);

HcsDidNetwork didNetwork = HcsDidNetwork.fromHcsDid(client, HcsDid.fromString(did), maxFileQueryPayment);
```
### Decentralized Identifiers
Decentralized Identifiers based on Hedera DID Method must conform to its ABNF notation. SDK provides useful utilities that will take care of constructing a valid DID string for each network.

#### DID Generation
A DID is represented in SDK as `HcsDid` object and can be easily converted to it's DID string form by calling its `toDid()` or `toString()` method. New decentralized identifiers can be generated in multiple handly ways:
- from already instantiated network:
```java
HcsDidNetwork didNetwork = ...;

// From a given DID root key:
Ed25519PrivateKey didRootKey = ...;
HcsDid hcsDid = didNetwork.generateDid(didRootKey.publicKey, false);

// Without having a DID root key - it will be generated automatically:
// Here we decided to add DID topic ID parameter `tid` to the DID.
Entry<Ed25519PrivateKey, HcsDid> didRootKeyAndHcsDid = didNetwork.generateDid(true);

// Without having a DID root key - it will be generated automatically with secure random generator:
Entry<Ed25519PrivateKey, HcsDid> didRootKeyAndHcsDid2 = didNetwork.generateDid(SecureRandom.getInstanceStrong(), false);
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
As currently there is no standard Java library for DID documents, nor even JSON-LD version 1.1 documents, this SDK uses a custom `DidDocumentBase` class that constructs JSON-LD DID document with only mandatory DID document attributes defined by [W3C DID Specification](https://w3c.github.io/did-core/) and those required by [Hedera DID Method](https://github.com/hashgraph/identity-did). Appnet creators can extend `DidDocumentBase` class to include other attributes in the DID document or construct a JSON-LD string in their own way (e.g. using 3rd party RDF libraries).

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
CRUD operations on a given DID document are all executed in the same way, by using `HcsDidTransaction`.
The transaction is created from an instance of `HcsDidNetwork` by calling `createDidTransaction` method and specifying CRUD operation, e.g. `DidDocumentOperation.CREATE`. Then `HcsDidTransaction` must be provided with the DID document, which has to be signed by DID root key of the DID subject. Finally `ConsensusMessageSubmitTransaction` must be configured accordingly to Hedera SDK, built and signed.

Appnet implementations can optionally add a callback listener and receive an event when the DID message reached consensus and was propageted to the mirror node.
They can also have their own mirror node listener and catch incoming messages from DID topic.

Once confirmed DID document arrived from a mirror node, appnets shall store them in their own storage solution. This will allow them to resolve DIDs more efficiently, instead of querying mirror node for each request.

Here is an example DID document creation code:
```java
Client client = null;
MirrorClient mirrorClient = null;
HcsDidNetwork didNetwork = null;

Ed25519PrivateKey didRootKey = null;
HcsDid hcsDid = null;

String didDocument = hcsDid.generateDidDocument().toJson();

// Build and execute transaction
didNetwork.createDidTransaction(DidDocumentOperation.CREATE)
    // Provide DID document as JSON string
    .setDidDocument(didDocument)
    // Sign it with DID root key
    .signDidDocument(doc -> didRootKey.sign(doc))
    // Configure ConsensusMessageSubmitTransaction, build it and sign if required by DID topic
    .buildAndSignTransaction(tx -> tx.setMaxTransactionFee(new Hbar(2)).build(client))
    // Define callback function when consensus was reached and DID document came back from mirror node
    .onDidDocumentReceived((did, doc) -> {
      System.out.println("DID document published!");
      System.out.println(doc);
    })
    // Execute transaction
    .execute(client, mirrorClient);
```

##### Read (Resolve)
TODO: to be documented once implementation is finalized

### Verifiable Credentials
TODO: to be documented once VC implementation is ready

#### Issuing
#### Revocation
#### Verification

## License Information
Licensed under Apache License, Version 2.0 – see [LICENSE](LICENSE) in this repo or on the official Apache page  <http://www.apache.org/licenses/LICENSE-2.0>

## References
* <https://github.com/hashgraph/identity-did>
* <https://github.com/hashgraph/hedera-sdk-java>
* <https://docs.hedera.com/hedera-api/>
* <https://www.hedera.com/>
* <https://w3c-ccg.github.io/did-spec/>
* <https://www.w3.org/TR/vc-data-model/>
