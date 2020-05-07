# Identity Network - User Guide

---

- [Identity Network - User Guide](#identity-network---user-guide)
  - [Creation](#creation)
  - [Existing Network Instantiation](#existing-network-instantiation)

---

## Creation

An identity appnet should have the following artifacts created on the Hedera mainnet:

- [Address book][address-book] for the members of the appnet stored as a file in Hedera File Service
- Hedera Consensus Service topic for DID Document messages. HCS messages creating, updating, or deleting DID Documents are submitted to this topic.
- Hedera Consensus Service topic for Verifiable Credentials messages. HCS messages issuing, suspending, or revoking Verifiable Credentials are submitted to this topic.

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

## Existing Network Instantiation

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

[address-book]: https://github.com/hashgraph/did-method/blob/master/did-method-specification.md#appnet-address-book