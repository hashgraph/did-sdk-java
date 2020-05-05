
# Hedera™ Hashgraph DID - Java SDK

[![License: Apache-2.0](https://img.shields.io/badge/license-Apache--2.0-green)](LICENSE) [![Documentation](https://img.shields.io/badge/javadoc-reference-informational)](docs/sdk-javadocs/index.html)

![LINE](https://img.shields.io/badge/line--coverage-84%25-brightgreen.svg) ![INSTRUCTION](https://img.shields.io/badge/instruction--coverage-85%25-brightgreen.svg) ![METHOD](https://img.shields.io/badge/method--coverage-86%25-brightgreen.svg) ![CLASS](https://img.shields.io/badge/class--coverage-97%25-brightgreen.svg) ![COMPLEXITY](https://img.shields.io/badge/complexity-1.95-brightgreen.svg)

This repository contains the Java SDK for managing DID Documents & Verifiable Credentials framework using the Hedera Consensus Service.

This SDK is designed to simplify :

- creation of identity networks within appnets,
- generation of decentralized identifiers for [Hedera DID Method][did-method-spec],
- creation, update, deletion and resolution of DID documents in appnet identity networks,
- issuance, revocation and status verification of [Verifiable Credentials][vc-data-model].

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
    - [Running Example Appnet](#running-example-appnet)
  - [Contributing](#contributing)
  - [License Information](#license-information)
  - [References](#references)

## Overview

TODO: this paragraph is work in progress...

## Usage

### Dependency Declaration

TODO: To be updated after release to MVN Repository

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
- [SDK JavaDoc Reference](/docs/sdk-javadocs/index.html)

### Getting Started Guides

- [Identity Network](/docs/id-network-user-guide.md)
- [Decentralized Identifiers](/docs/did-user-guide.md)
- [Verifiable Credentials Registry](/docs/vc-user-guide.md)

### Running Example Appnet

The `/examples/appnet-api-server` folder contains an example implementation of an appnet that utilizes DID and VC SDK and exposes a REST API interface according to the Hedera DID Method Specification. The appnet server can be started by the following command directly from the root folder of this repository:

```cmd
gradle :appnet-api-server:run
```

The appnet runs on localhost port 5050 be default. It does not expose any user interface, instead there is a collection of POSTMAN requests available [here](/examples/appnet-api-server/postman-example-requests/e2e-flow.postman_collection) that demonstrate a full end-to-end flow of DID documents generation, publishing, update and deletion, as well as verifiable credential generation, issuance and revocation.

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
- <https://w3c-ccg.github.io/did-spec/>
- <https://www.w3.org/TR/vc-data-model/>

[did-method-spec]: https://github.com/hashgraph/did-method
[vc-data-model]: https://www.w3.org/TR/vc-data-model/