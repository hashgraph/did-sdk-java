
# Hedera™ Hashgraph DID - Java SDK

[![License: Apache-2.0](https://img.shields.io/badge/license-Apache--2.0-green)](LICENSE) [![Documentation](https://img.shields.io/badge/javadoc-reference-informational)](docs/sdk-javadocs/index.html)

![LINE](https://img.shields.io/badge/line--coverage-84%25-brightgreen.svg) ![INSTRUCTION](https://img.shields.io/badge/instruction--coverage-85%25-brightgreen.svg) ![METHOD](https://img.shields.io/badge/method--coverage-86%25-brightgreen.svg) ![CLASS](https://img.shields.io/badge/class--coverage-97%25-brightgreen.svg) ![COMPLEXITY](https://img.shields.io/badge/complexity-1.95-brightgreen.svg)

This repository contains the Java SDK for managing [DID Documents][did-core] & [Verifiable Credentials][vc-data-model] registry using the Hedera Consensus Service.

## Table of Contents

- [Hedera™ Hashgraph DID - Java SDK](#hedera%e2%84%a2-hashgraph-did---java-sdk)
  - [Table of Contents](#table-of-contents)
  - [Overview](#overview)
  - [Usage](#usage)
    - [Dependency Declaration](#dependency-declaration)
      - [Maven](#maven)
      - [Gradle](#gradle)
    - [Documentation](#documentation)
    - [Getting Started Guides](#getting-started-guides)
    - [Examples](#examples)
  - [Contributing](#contributing)
  - [License Information](#license-information)
  - [References](#references)

## Overview

Identity networks are set of artifacts on Hedera Consensus Service that allow applications to share common channels to publish and resolve DID documents, issue verifiable credentials and control their validity status. These artifacts include:

- address book - a file on Hedera File Service that provides information about HCS topics and appnet servers,
- DID topic - an HCS topic intended for publishing DID documents,
- and VC topic - an HCS topic playing a role of verifiable credentials registry.

This SDK is designed to simplify :

- creation of identity networks within appnets, that is: creation and initialization of the artifacts mentioned above,
- generation of decentralized identifiers for [Hedera DID Method][did-method-spec] and creation of their basic DID documents,
- creation (publishing), update, deletion and resolution of DID documents in appnet identity networks,
- issuance, revocation and status verification of [Verifiable Credentials][vc-data-model].

The SDK does not impose any particular way of how the DID or verifiable credential documents are constructed. Each appnet creators can choose their best way of creating those documents and as long as these are valid JSON-LD files adhering to W3C standards, they will be handled by the SDK.

## Usage

### Dependency Declaration

> TODO: To be updated after release to MVN Repository.

#### Maven

```xml
<dependency>
  <groupId>com.hedera.hashgraph</groupId>
  <artifactId>identity</artifactId>
  <version>1.0.0</version>
</dependency>
```

#### Gradle

```gradle
implementation group: 'com.hedera.hashgraph', name: 'identity', version: '1.0.0'
```

### Documentation

- [DID Method Specification][did-method-spec]
- [Verifiable Credentials Registry](/docs/vc-specification.md)
- [SDK JavaDoc Reference][sdk-javadocs]

### Getting Started Guides

- [Identity Network](/docs/id-network-user-guide.md)
- [Decentralized Identifiers](/docs/did-user-guide.md)
- [Verifiable Credentials Registry](/docs/vc-user-guide.md)

### Examples

The `/examples/appnet-api-server` folder contains an example implementation of an appnet that utilizes DID and VC SDK and exposes a REST API interface according to the Hedera DID Method Specification. The appnet server can be started by the following command directly from the root folder of this repository:

```cmd
gradle :appnet-api-server:run
```

Please refer to the [README](/examples/appnet-api-server/README.md) file of the appnet project for more details.

## Contributing

We welcome participation from all developers! For instructions on how to contribute to this repo, please review the [Contributing Guide](/CONTRIBUTING.md).

## License Information

Licensed under [Apache License, Version 2.0](LICENSE).

## References

- <https://github.com/hashgraph/did-method>
- <https://github.com/hashgraph/hedera-sdk-java>
- <https://docs.hedera.com/hedera-api/>
- <https://www.hedera.com/>
- <https://www.w3.org/TR/did-core/>
- <https://www.w3.org/TR/vc-data-model/>

[did-method-spec]: https://github.com/hashgraph/did-method
[did-core]: https://www.w3.org/TR/did-core/
[vc-data-model]: https://www.w3.org/TR/vc-data-model/
[sdk-javadocs]: https://hashgraph.github.io/did-sdk-java/sdk-javadocs/
