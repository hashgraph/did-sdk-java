# Verifiable Credentials Registry

---
- [Verifiable Credentials Registry](#verifiable-credentials-registry)
  - [About](#about)
  - [Credential Hash](#credential-hash)
  - [Registering Verifiable Credential Status](#registering-verifiable-credential-status)
    - [Issuance, Suspension, Resumption, Revocation](#issuance-suspension-resumption-revocation)
    - [Status Verification](#status-verification)
    - [Appnet Relay Interface for Verifiable Credentials Registry](#appnet-relay-interface-for-verifiable-credentials-registry)
      - [Issue Service](#issue-service)
      - [Read Service](#read-service)
      - [Suspend Service](#suspend-service)
      - [Resume Service](#resume-service)
      - [Revoke Service](#revoke-service)
      - [Submit Service](#submit-service)
  - [Security and Privacy Considerations](#security-and-privacy-considerations)
  - [Reference Implementations](#reference-implementations)
  - [References](#references)

---

## About

This document defines a binding of the [Verifiable Credentials][w3c-vc] (VC) Registry architecture to Hedera Hashgraph - specifically how to use the Hedera Consensus Service (HCS) for issuance and revocation of verifiable credentials within appnet identity networks based on [Hedera DID method][did-method]. An appnet is a network of computers that store some set of business data (such as DID Documents) in a shared state, and rely on the Hedera mainnet for timestamping and ordering the transactions that cause that appnet state to change.

Verifiable Credentials Registry never exposes Personally Identifiable Information nor sends credential documents to the hedera network.
Instead, it provides a trusted platform to trace and verify changes to credential document status such as suspension or revocation.

## Credential Hash

Hedera VC registry operates on credential hashes that are derived from verifiable credential documents by calculating a hash of a subset of this document attributes that are constant and mandatory. In order for verifiers to be able to resolve the status of a credential they must be able to calculate its hash without having access to all credential subjects or claim attributes in the document (in case they were shared with them partially). For this reason the credential hash is defined as a Base64-encoded SHA-256 hash of a normalized JSON document consisting of the following attributes of the original VC document:

- `id` - even though VC document specification does not specify it as mandatory, it is on Hedera VC registry and every VC should define one.
- `type` - a list of verifiable credential type values.
- `issuer` - the DID of the issuer.
- `issuanceDate` - the date when credential was issued. The value MUST be a valid XML datetime value, as defined in section 3.3.7 of [W3C XML Schema Definition Language (XSD) 1.1 Part 2: Datatypes](https://www.w3.org/TR/xmlschema11-2/). This datetime value MUST be normalized to UTC 00:00, as indicated by the trailing "Z".

Example minified JSON:

```json
{"id":"example:test:vc:id","type":["VerifiableCredential"],"issuer":"did:hedera:testnet:8pYs7oiAjph8AFRJsRoFTfwgaWww7ytpSJ8beT3E353y;hedera:testnet:fid=0.0.23759","issuanceDate":"2020-05-06T09:53:22.070Z"}
```

## Registering Verifiable Credential Status

Hedera network nodes support a Consensus Service API by which clients submit transaction messages to a topic - these transactions assigned a consensus timestamp and order before flowing back out to mirror nodes and any appnets subscribed to the relevant topic. Every appnet that implements the Hedera Verifiable Credentials Registry must have a dedicated HCS topic created for VC status registration. Members of the appnet will subscribe to the corresponding topics, and consequently retrieve and store the latest valid VC status submitted to the topic.

Issue, Suspend, Resume and Revoke operations against a VC hash are submitted via the Consensus Service API, either directly by a VC issuer or indirectly by an appnet member on behalf of a VC issuer.

A valid Issue, Suspend, Resume or Revoke message must have a JSON structure defined by a [vc-message-schema](/docs/vc-message.schema.json) and must contains the following properties:

- `mode` - Describes the mode in which the message content is provided. Valid values are: `plain` or `encrypted`. Messages in `encrypted` mode have `credentialHash` attribute encrypted. Other attributes are plain.
- `message` - The message content with the following attributes:
  - `operation` - VC operation to be performed on the VC document.  Valid values are: `issue`, `suspend`, `resume` and `revoke`.
  - `credentialHash` -  - This field may contain either:

    - a plain credential hash,
    - or a Base64-encoded encrypted representation of the credential hash, where the encryption and decryption methods and keys are defined by appnet owners.
  - `timestamp` - A message creation time. The value MUST be a valid XML datetime value, as defined in section 3.3.7 of [W3C XML Schema Definition Language (XSD) 1.1 Part 2: Datatypes](https://www.w3.org/TR/xmlschema11-2/). This datetime value MUST be normalized to UTC 00:00, as indicated by the trailing "Z". It is important to note that this timestamp is a system timestamp as a variable part of the message and does not represent a consensus timestamp of a message submitted to the VC topic.
- `signature` - A Base64-encoded signature that is a result of signing a minified JSON string of a message attribute with a private key corresponding to the public key `#did-root-key` of the signing subject.

Neither the Hedera network nor mirror nodes validate the messages against the above requirements - it is appnets that, as part of their subscription logic, must validate VC messages based on the above criteria. The messages with duplicate signatures shall be disregarded and only the first one with verified signature is valid (in consensus timestamp order).

Here is an example of a complete message wrapped in an envelope and signed:

```json
{
   "mode":"plain",
   "message":{
      "operation":"issue",
      "credentialHash":"GmcA3ut3tM7d51tRjkxvGZNaigtzwFAVunkimQC573Mv",
      "timestamp":"2020-05-06T16:45:20.006Z"
   },
   "signature":"8sCN9zIUcIL17R9KILvPoAkYkh8XFOCGL+uIYZw+nfDZuuEEb0JZHMZ/78Anr1QrKx9KLBxd3c6Xay8gNZ//AA=="
}
```

It is a responsibility of an appnet's administrators to decide who can submit messages to their VC topic. Access control of message submission is defined by a `submitKey` property of `ConsensusCreateTopicTransaction` body. If no `submitKey` is defined for a topic, then any party can submit messages against the topic. Detailed information on Hedera Consensus Service APIs can be found in the official [Hedera API documentation](https://docs.hedera.com/hedera-api/consensus/consensusservice).

### Issuance, Suspension, Resumption, Revocation

Each of these operations defines a new status of a verifiable credential document and is registered within a particular appnet by sending a `ConsensusSubmitMessage` transaction to a Hedera network node. It is executed by sending a `submitMessage` RPC call to the HCS API with the `ConsensusSubmitMessageTransactionBody` containing:

- `topicID` - equal to the ID of appnet's VC topic
- `message` - a JSON VC message envelope described above with `operation` set to the corresponding event type:

  - `issue`
  - `suspend`
  - `resume`
  - `revoke`

Appnet members subscribed to this VC topic shall store the credential hash and its status (latest operation) in their local storage upon receiving this message from a mirror. Appnets shall check if credential document before receiving this message was in fact in a state that allows transition to the new state. For example: no status changes are allowed after verifiable credential has been revoked.

### Status Verification

Verifiers, when presented clams from the subject owner must validate the document in terms of it's content and clam values according to their requirements. But as well the proofs within the verifiable credential document. As there are multiple [types of proofs][w3c-proofs-registry] available, this part is not subject to SDK functionality and shall be done off-chain by the verifiers. Once these checks has been passed successfully verifiers must ensure that credentials presented to them were not suspended or revoked. This verification is performed against Hedera Verifiable Credentials registry directly or the appnet REST API service.

How resolution of a VC status occurs depends on whether the operation registration was submitted in encrypted or plaintext mode and, where resolution happens.

If submitted in plaintext mode, then resolution can occur either against a mirror node that may have persisted the history of messages for that VC hash, or against a member of the appnet that persisted the status.

If resolved against a mirror, resolution requires reading the latest transaction message submitted to a VC topic for the given VC hash and checking if there was no earlier transaction for it with `operation` set to `revoke` or another message with the same `signature`. This method can only be executed on a mirror node and may require reading a full history of messages in the topic. Hedera mirror nodes provide a [Hedera Consensus Service gRPC API](https://docs.hedera.com/guides/docs/mirror-node-api/hedera-consensus-service-api-1). Client applications can use this API to subscribe to an appnet's VC topic to receive messages submitted to it and filter those that contain the VC hash status in question.

Here is an example Java client, please refer to the official Hedera documentation for more information:

```java
new MirrorConsensusTopicQuery()
    .setTopicId(vcTopicId)
    .subscribe(mirrorClient, resp -> {
          String vcMessage = new String(resp.message, StandardCharsets.UTF_8);
          System.out.println(resp.consensusTimestamp + " received VC message: " + vcMessage);
      },
      // On gRPC error, print the stack trace
      Throwable::printStackTrace);
    });
```

If resolved against an appnet member, resolution requires calling appnet relay service (defined below).

If the VC status registration messages were submitted in encrypted mode, then resolution against a mirror is not possible unless the verifier is in possession of a decryption key and the appnet service is the only way of resolution.

### Appnet Relay Interface for Verifiable Credentials Registry

VC Controllers, who have their own accounts on the Hedera network and are authorized to submit messages to an appnet's VC topic can send Issue, Suspend, Resume and Revoke HCS transactions directly to a Hedera network node. Alternatively, VC Controllers can use an appnet's relay interface that will send the HCS messages for them. The access to an appnet's relay interface is defined by each appnet owner, so authentication and authorization mechanisms are out of scope. This specification only defines a common REST API interface for each of those operations. The interface path is relative to the service URLs defined in the appnet's address book file.

The mode in which the messages are submitted into the VC topic is defined by the appnet and in case of encryption, it is the appnet that controls encryption and decryption keys. Due to that, submission of issue, suspend, resume or revoke messages is executed in two steps:

1. Message preparation
   - VC status controlling subject sends Issue/Suspend/Resume/Revoke request to the appnet with a VC hash as path parameter
   - Appnet prepares a message envelope taking care of encryption mode and operation type
   - Appnet responds with a message envelope without signature.
2. Message signing and submission
   - DID subject takes appnet's response envelope and signs the `message` content with their `#did-root-key`
   - DID subject sends Submit request to the appnet with the signed message envelope as request body.

All API operations shall have the same error response content as the following example:

```json
{
  "error" : {
    "code" : 404,
    "message" : "You are unauthorized to make this request."
  }
}
```

#### Issue Service

Requests that a new Verifiable Credential document be registered as Issued on the appnet network.

The Verifiable Credential document is not considered to have been issued until issuance was submitted to the HCS, received a consensus timestamp and order, been retrieved by the members of the appropriate appnet, and persisted in the appnet state.

The actual exchange of a Verifiable Credential document between the issuer and its owner is an off-chain process that does not involve Hedera identity network and is define by those parties.

The mode in which the VC message is submitted (plain or encrypted) is defined on the appnet level and shall be transparent to the API users.

- __URL:__ `/vc/{credentialHash}`
- __Method:__ `POST`
- __Content Type:__ `application/json`
- __URL Parameters:__ *None*
- __Path Parameters:__ `credentialHash` - a hash of a VC document, calculated as described above
- __Request Body:__ *None*
- __Success Response:__
  - __Code:__ `200 OK`
  - __Response Body:__ Unsigned message envelope, e.g.:
  
```json
{
    "mode": "plain",
    "message": {
        "operation": "issue",
        "credentialHash": "9wsnFSSxQoibcpV2D11HwWRcsej53AVZ5jSXDQgD6Uh",
        "timestamp": "2020-05-07T08:00:02.683Z"
    }
}
```

- __Error Response:__
  - __Code:__ `401 UNAUTHORIZED`
  - __Code:__ `500 INTERNAL SERVER ERROR`

#### Read Service

Reads the latest valid status of a given credential hash registered in the VC topic. 
The appnet shall inject the consensus timestamp the latest valid message into the `updated` property of the returned message.

- __URL:__ `/vc/{credentialHash}`
- __Method:__ `GET`
- __Content Type:__ `application/json`
- __URL Parameters:__ *None*
- __Path Parameters:__ `credentialHash` - a hash of a VC document, calculated as described above
- __Request Body:__ *None*
- __Success Response:__
  - __Code:__ `200 OK`
  - __Response Body:__
The latest status of a verifiable credential document in a plain form including consensus timestamp of this registered message as `updated` property:

```json
{
    "operation": "issue",
    "credentialHash": "GwGDzNmwAXmmzkoLhMGeupSk2e511aF3nxUHc7wnPQh2",
    "timestamp": "2020-05-07T08:07:40.083Z",
    "updated": "2020-05-07T08:07:41.012Z"
}
```

- __Error Response:__
  - __Code:__ `401 UNAUTHORIZED`
  - __Code:__ `404 NOT FOUND` - in case credential hash was not found in the registry.
  - __Code:__ `500 INTERNAL SERVER ERROR`

#### Suspend Service

Requests that a Verifiable Credential be registered as Suspended on the appnet network.

- __URL:__ `/vc/{credentialHash}`
- __Method:__ `PUT`
- __Content Type:__ `application/json`
- __URL Parameters:__ *None*
- __Path Parameters:__ `credentialHash` - a hash of a VC document, calculated as described above
- __Request Body:__ *None*
- __Success Response:__
  - __Code:__ `200 OK`
  - __Response Body:__ Unsigned message envelope, e.g.:
  
```json
{
    "mode": "plain",
    "message": {
        "operation": "suspend",
        "credentialHash": "9wsnFSSxQoibcpV2D11HwWRcsej53AVZ5jSXDQgD6Uh",
        "timestamp": "2020-05-07T08:00:02.683Z"
    }
}
```

- __Error Response:__
  - __Code:__ `401 UNAUTHORIZED`
  - __Code:__ `500 INTERNAL SERVER ERROR`

#### Resume Service

Requests that a Verifiable Credential previously suspended be registered as Resumed on the appnet network.

- __URL:__ `/vc/{credentialHash}`
- __Method:__ `PATCH`
- __Content Type:__ `application/json`
- __URL Parameters:__ *None*
- __Path Parameters:__ `credentialHash` - a hash of a VC document, calculated as described above
- __Request Body:__ *None*
- __Success Response:__
  - __Code:__ `200 OK`
  - __Response Body:__ Unsigned message envelope, e.g.:
  
```json
{
    "mode": "plain",
    "message": {
        "operation": "resume",
        "credentialHash": "9wsnFSSxQoibcpV2D11HwWRcsej53AVZ5jSXDQgD6Uh",
        "timestamp": "2020-05-07T08:00:02.683Z"
    }
}
```

- __Error Response:__
  - __Code:__ `401 UNAUTHORIZED`
  - __Code:__ `500 INTERNAL SERVER ERROR`

#### Revoke Service

Requests that a Verifiable Credential be registered as irreversibly Revoked on the appnet network.
Once this message is sent, all future status changes of the VC document shall be ignored by appnets.

- __URL:__ `/vc/{credentialHash}`
- __Method:__ `DELETE`
- __Content Type:__ `application/json`
- __URL Parameters:__ *None*
- __Path Parameters:__ `credentialHash` - a hash of a VC document, calculated as described above
- __Request Body:__ *None*
- __Success Response:__
  - __Code:__ `200 OK`
  - __Response Body:__ Unsigned message envelope, e.g.:
  
```json
{
    "mode": "plain",
    "message": {
        "operation": "delete",
        "credentialHash": "9wsnFSSxQoibcpV2D11HwWRcsej53AVZ5jSXDQgD6Uh",
        "timestamp": "2020-05-07T08:00:02.683Z"
    }
}
```

- __Error Response:__
  - __Code:__ `401 UNAUTHORIZED`
  - __Code:__ `500 INTERNAL SERVER ERROR`
  
#### Submit Service

Requests that a given signed VC status registration message prepared by one of the services mentioned above be submitted to the VC topic of Hedera network.

- __URL:__ `/vc-submit`
- __Method:__ `POST`
- __Content Type:__ `application/json`
- __URL Parameters:__ *None*
- __Request Body:__ A message envelope prepared by the appnet in Issue, Suspend, Resume or Revoke request and signed by DID subject with their  `#did-root-key`, e.g.:

```json
{
    "mode": "plain",
    "message": {
        "operation": "issue",
        "credentialHash": "GwGDzNmwAXmmzkoLhMGeupSk2e511aF3nxUHc7wnPQh2",
        "timestamp": "2020-05-07T08:07:40.083Z"
    },
    "signature": "MWXVXNiLjWWcxiVLIvixPcJUDBXlKBSAzviPFzgexxQ2NGwn/xgLNa/vXqoPo70VscOLhIgTclOE4fEnezz/AQ=="
}
```

- __Success Response:__
  - __Code:__ `202 ACCEPTED`
- __Error Response:__
  - __Code:__ `401 UNAUTHORIZED`
  - __Code:__ `500 INTERNAL SERVER ERROR`

## Security and Privacy Considerations

Security of Hedera Verifiable Credentials Registries inherits the security properties of Hedera Hashgraph network itself and specific implementation of appnets that persist the Credential Hash and its status.

If it be undesirable that a VC document status changes history be public, it can be encrypted such that only members of an appnet can read it, or even specific members of the appnet. HCS supports perfect forward secrecy through key rotation to mitigate the risk of encrypted credential hashes persisted on mirrors being decrypted.

Write access to Hedera Consensus Service VC Topics can be controlled by stipulating a list of public keys for the topic by appnet administrators. Only HCS messages signed by the corresponding private keys will be accepted. A key can be a "threshold key", which means a list of M keys, any N of which must sign in order for the threshold signature to be considered valid. The keys within a threshold signature may themselves be threshold signatures, to allow complex signature requirements.

Regardless of encryption, a VC message submitted to Hedera Consensus Service topic never contains Personally Identifiable Information (PII).

## Reference Implementations

The code at [https://github.com/hashgraph/did-sdk-java](https://github.com/hashgraph/did-sdk-java) is intended to provide a Java SDK for this specification. A set of unit tests and [example appnet application](/examples/appnet-api-server/README.md) within this repository present a reference implementation of a VC registry.

## References

- <https://github.com/hashgraph/did-method>
- <https://github.com/hashgraph/hedera-sdk-java>
- <https://docs.hedera.com/hedera-api/>
- <https://www.hedera.com/>
- <https://www.w3.org/TR/vc-data-model/>

[w3c-vc]: https://www.w3.org/TR/vc-data-model/
[did-method]: https://github.com/hashgraph/did-method