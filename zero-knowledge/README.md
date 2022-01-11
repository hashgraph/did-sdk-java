# Introduction
This module gives some basic tools to take advantage of the zero knowledge feature.
It is composed of two main parts:
- the standardized documents (verifiable document and presentation) and their representations;
- the tools to generate a zero knowledge proof to include.

## Formal documents
### Verifiable credential
It offers a base concrete class that leverages the zero knowledge feature.

### Verifiable presentation
It offers the base structure to create a verifiable presentation document, all the tools to **generate** one from one or more verifiable credential documents and their **marshaller** (objects whose responsibility is to translate from the internal representation of a document to a standardized formatted one, such as stated in the w3c standard).

## Zero knowledge tools
The zero knowledge feature is represented both in the verifiable credential and verifiable presentation document:
- vc doc: the merkle tree root and a zero knowledge signature are placed in the proof section;
- vp doc: the vc's document zk signature is copied in the relative verifiable credential section, along

#### Merkle tree root
The credential subject field inside a verifiable credential document is also represented as a merkle tree; each element is added as a leaf in the tree
as a hash of the label and the value. The root of the tree is then added in the proof section of the verifiable credential document. 

#### Zero knowledge signature
A signature that is included in the proof section of a verifiable credential and is computed signing with the issuer's private key
the hash of the document id and the merkle tree root.

### Circuit
The circuit is a tool used to generate and verify a zero knowledge snark proof. The module provides some interfaces
to interact with a general circuit.

## Usage examples
### Get a formatted json from a verifiable credential document 
You need to create a marshaller that contains the logic to translate between the internal representation and 
the formatted json and the other way around.
```
HcsVcDocumentMarshaller<VcDocument> vcMarshaller = new HcsVcDocumentMarshaller<>(); 
String vcDocAsJsonString = vcMarshaller.fromDocumentToString(vcDoc);
```

### Get a verifiable credential document from formatted json
As above, but the other way around.
```
HcsVcDocumentMarshaller<VcDocument> vcMarshaller = new HcsVcDocumentMarshaller<>(); 
HcsVcDocument vcDoc = vcMarshaller.fromStringToDocument(vcDocAsJsonString);
```

### Get a formatted json from a verifiable presentation document
The same as for a verifiable credential document.
```
HcsVpDocumentMarshaller<VpDocument> vpMarshaller = new HcsVpDocumentMarshaller<>(); 
String vpDocAsJsonString = vpMarshaller.fromDocumentToString(vpDoc);
```

### Get a verifiable presentation document from formatted json
As above, but the other way around.
```
HcsVpDocumentMarshaller<VpDocument> vpMarshaller = new HcsVpDocumentMarshaller<>(); 
HcsVpDocument vpDoc = vpMarshaller.fromStringToDocument(vpDocAsJsonString);
``` 

## Dependencies
At the moment, the module is using a local dependency `sc-common-cryptolib-0.1.0.jar`, based on the 
public repo [sc_cryptolib_common](https://github.com/HorizenOfficial/sc_cryptolib_common). 
Just run the [script](https://github.com/HorizenOfficial/sc_cryptolib_common/blob/master/build/build_jar_tweedle.sh) to obtain the compiled jar. 