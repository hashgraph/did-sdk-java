# Verifiable Credentials Registry - User Guide

```NOTE: This document is work in progress - not ready for a review.```

- [Verifiable Credentials Registry - User Guide](#verifiable-credentials-registry---user-guide)
  - [Status Registration](#status-registration)
  - [Status Verification](#status-verification)

## Verifiable Credential Document Construction

### Credential Schema

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

  public ExampleCredential(String ownerDid, String firstName, String lastName, int age) {
    this.id = ownerDid;
    this.firstName = firstName;
    this.lastName = lastName;
    this.age = age;
  }

  ...
}
```

### Credential Hash Calculation

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

## Status Registration

## Status Verification
