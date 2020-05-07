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
If it is missing, the application will create identity network artifacts upon startup and print them into system console:

- `EXISTING_ADDRESS_BOOK_JSON` - identity network's address book as JSON string. If provided appnet will not query Hedera File Service for its content.
- `EXISTING_ADDRESS_BOOK_FILE_ID` - File ID of the address book on Hedera File Service.

[did-method-spec]: https://github.com/hashgraph/did-method
[postman]: https://www.postman.com/