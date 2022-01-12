# Example Appnet Implementation

---
- [Example Appnet Implementation](#example-appnet-implementation)
  - [About](#about)
  - [Usage](#usage)
  - [Configuration](#configuration)

---

## About

This project is an example of how appnets can build identity networks on top of Hedera and utilize Hedera DID Method and Verifiable Credentials Registry. It is not a production-grade reference implementation, as this appnet exchanges and exposes private keys of issuers and credential owners via its API interface for demonstration purposes. It also does not use any persistent storage, clears all its data upon shutdown and listens to DID and VC topics from startup, ignoring past messages - so that each execution has a 'clean' demo state.

## Usage

The appnet server can be started by the following command directly from the root folder of this repository:

```cmd
gradle :appnet-api-server:run
```

The appnet runs on localhost port 5050 be default. It does not expose any user interface, instead there is a collection of [Postman][postman] requests available [here](/examples/appnet-api-server/postman-example-requests/e2e-flow.postman_collection) that demonstrate a full end-to-end flow of DID documents generation, publishing, update and deletion, as well as verifiable credential generation, issuance and revocation.

There are three types of requests in this collection:

- `DID` - reference requests of Appnet Relay Service as defined in [Hedera DID Method Specification][did-method-spec]
- `VC` - reference requests of Appnet Relay Service as defined in [Verifiable Credentials Registry](/docs/vc-specification.md)
- `DEMO` - additional requests for demonstration purposes. These in real-world application would be implemented differently, in secure environment (e.g. in a client wallet application).

The collection is ready to be run end-to-end and automatically captures responses from previous requests as input for the next.

## Configuration

The following environment variables are required to be set up before running the application:

- `OPERATOR_ID` - Your testnet account ID.
- `OPERATOR_KEY` - Your testnet account ID private key
- `MIRROR_NODE_ADDRESS` - Address of the mirror node this application should connect to


Additionally the following configuration of already initialized identity network can be provided.
If it is missing, the application will create identity network artifacts upon startup and print them into system console. A new Hedera file containing the address book and two topic IDs will be created.

If the file id below alone is provided, it will be used to fetch the VC and DiD topic IDs from the Hedera Network
- `EXISTING_ADDRESS_BOOK_FILE_ID` (e.g. 0.0.19087)

If the below is provided, topic IDs won't be fetched from the network, saving the cost of a file query
- `EXISTING_ADDRESS_BOOK_JSON` (e.g. {"appnetName":"Example appnet using Hedera Identity SDK","didTopicId":"0.0.19085","vcTopicId":"0.0.19086","appnetDidServers":["http://localhost:5050/"]})

Finally, the following two optional parameters may be set (they will default to 10 if unset). They determine how frequently DiDs and VCs are persisted to file. If for example the value is 5, then every 5 VC operations, the state of the VCs will be persisted.
Providing a large number may improve performance due to fewer file operations, however upon restart, catching up with mirror node to rebuild state to its latest may take longer.

- `DID_PERSIST_INTERVAL` - how frequently should DiDs be persisted to file
- `VC_PERSIST_INTERVAL` - how frequently should VCs be persisted to file

Persisted data resides in the `persistedCredentialIssuers.ser`, `persistedDiDs.ser`, `persistedSignatures.ser` and `persistedVCs.ser` of the application's folder. They are binary files and not human readable.

## Zero knowledge flow example
Roles:
- the authority: some entity allowed issuing verifiable credential document;
- the prover: a user who asks the authority for a verifiable credential document; can create a verifiable presentation
  to prove some statement;
- the verifier: whoever wants to verify some statement claimed by a prover through a verifiable presentation.

Example flow with API endpoints in `e2e-flow-with-zero-knowledge.postman_collection`:

_Notes_: to generate and verify a snark proof, the circuit needs a couple of keys: a proving and a verification key.
These keys will be generated once per circuit and everytime an entity needs to generate or verify a proof is going to use
them. To run the demo, run just once the main in `/examples/appnet-api-server/src/main/java/com/hedera/hashgraph/identity/hcs/example/appnet/ZeroKnowledgeKeyGenerator.java`.

1. An authority creates a public/secret key pair (in this case the Tweedle curve is used to generate such keys) -> `0.zk DEMO - Generate AUTHORITY schnorr key pair`;
2. It then creates a did document containing also the public key previously generated -> `1.zk DEMO - Generate DID for Issuer`;
3. It posts a message containing the _credential hash_ of the did document on Hedera -> `api calls from 2 through 4`;
4. A prover, in this case a user, wants the authority to release them a document certifying their age;
5. The user creates a public/secret key pair -> `0.zk DEMO - Generate HOLDER schnorr key pair`;
6. The user then creates their own did and post it to Hedera -> `6.zk DEMO - Generate DID with ZK for Owner`;
7. The authority then issues a verifiable credential document to the user, where their personal data are reported in plain text -> `7.zk DEMO - Generate ZK Driving License document`;
8. A verifier, in this example a merchant, is offering some service, but only to people older than 18 years old;
9. The user is interested in such service and has a verifiable credential document stating their age;
10. The merchant sends a challenge to the user (this is just a simplification for this example);
11. The user then generates a presentation out of the vc document, where a snark proof is stating the above-age claim -> `25.zk VP - Get driver above age presentation`;
12. The merchant is sent the presentation, where no user's personal data is included, and can verify the proof telling whether the user is telling the truth or not -> `27.zk VP - Verify presentation`.

## Dependencies
The example is using `hedera-cryptolib-0.1.0.jar`, a jar compiled from a private repo: at the moment the jar is located in `did-sdk-java/examples/appnet-api-server/src/main/resources/jars/x86/hedera-cryptolib-0.3.0.jar`. 

[did-method-spec]: https://github.com/hashgraph/did-method
[postman]: https://www.postman.com/
