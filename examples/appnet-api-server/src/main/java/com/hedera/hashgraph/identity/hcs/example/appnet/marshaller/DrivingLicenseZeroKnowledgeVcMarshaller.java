package com.hedera.hashgraph.identity.hcs.example.appnet.marshaller;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hedera.hashgraph.identity.hcs.example.appnet.dto.BirthDate;
import com.hedera.hashgraph.identity.hcs.example.appnet.vc.CredentialSchema;
import com.hedera.hashgraph.identity.hcs.example.appnet.vc.DrivingLicense;
import com.hedera.hashgraph.identity.hcs.example.appnet.vc.DrivingLicenseZeroKnowledgeDocument;
import com.hedera.hashgraph.identity.hcs.example.appnet.vc.Ed25519CredentialProof;
import com.hedera.hashgraph.identity.hcs.vc.marshaller.HcsVcDocumentMarshaller;
import com.hedera.hashgraph.identity.hcs.vc.HcsVcDocumentJsonProperties;
import com.hedera.hashgraph.zeroknowledge.merkletree.factory.MerkleTreeFactoryImpl;
import com.hedera.hashgraph.zeroknowledge.vc.HcsVcDocumentZeroKnowledgeJsonProperties;
import com.hedera.hashgraph.zeroknowledge.vp.proof.ZeroKnowledgeSignature;
import com.hedera.hashgraph.zeroknowledge.vp.proof.ZkSignature;
import org.threeten.bp.Instant;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static com.hedera.hashgraph.identity.hcs.example.appnet.dto.BirthDate.getJsonElementAsBirthDate;
import static com.hedera.hashgraph.identity.utils.JsonUtils.getJsonElementAsList;

public class DrivingLicenseZeroKnowledgeVcMarshaller extends HcsVcDocumentMarshaller<DrivingLicenseZeroKnowledgeDocument> {
    private static final String[] JSON_PROPERTIES_ORDER = {"@context", "id", "type", "credentialSchema",
            "credentialSubject", "issuer", "issuanceDate", "proof"};

    @Override
    public String fromDocumentToString(DrivingLicenseZeroKnowledgeDocument vcDocument) {
        JsonObject root = GSON.toJsonTree(vcDocument).getAsJsonObject();

        LinkedHashMap<String, JsonElement> map = new LinkedHashMap<>();
        for (String property : JSON_PROPERTIES_ORDER) {
            if (property.equals(HcsVcDocumentJsonProperties.PROOF) && root.get(property) != null) {
                insertProofProperty(vcDocument, root, map, property);
            } else if (HcsVcDocumentJsonProperties.CREDENTIAL_SUBJECT.equals(property) && vcDocument.getCredentialSubject() != null) {
                insertDrivingLicenseProperty(vcDocument, map, property);
            } else if (root.has(property)) {
                map.put(property, root.get(property));
            }
        }

        return GSON.toJson(map);
    }

    private void insertDrivingLicenseProperty(DrivingLicenseZeroKnowledgeDocument vcDocument, LinkedHashMap<String, JsonElement> map, String property) {
        JsonArray credentialSubjectsArray = new JsonArray();

        for (DrivingLicense dl : vcDocument.getCredentialSubject()) {
            credentialSubjectsArray.add(dl.toNormalizedJsonElement());
        }

        map.put(property, credentialSubjectsArray);
    }

    private void insertProofProperty(DrivingLicenseZeroKnowledgeDocument vcDocument, JsonObject root, LinkedHashMap<String, JsonElement> map, String property) {
        JsonObject proofObject = root.get(property).getAsJsonObject();
        ZeroKnowledgeSignature<DrivingLicense> zeroKnowledgeSignature = vcDocument.getZeroKnowledgeSignature();
        proofObject.addProperty(HcsVcDocumentZeroKnowledgeJsonProperties.ZK_SIGNATURE, zeroKnowledgeSignature.getSignature());
        proofObject.addProperty(HcsVcDocumentZeroKnowledgeJsonProperties.MERKLE_TREE_ROOT, zeroKnowledgeSignature.getMerkleTreeRoot());
        map.put(property, proofObject);
    }
    @Override
    public DrivingLicenseZeroKnowledgeDocument fromJsonToDocument(JsonObject jsonDocument) {
        DrivingLicenseZeroKnowledgeDocument document = super.fromJsonToDocument(jsonDocument);

        List<DrivingLicense> drivingLicenses = translateToDriverLicense(jsonDocument.get(HcsVcDocumentJsonProperties.CREDENTIAL_SUBJECT));
        document.setCredentialSubject(drivingLicenses);

        JsonObject proofJson = jsonDocument.get(HcsVcDocumentJsonProperties.PROOF).getAsJsonObject();
        String zkSignature = proofJson.get(HcsVcDocumentZeroKnowledgeJsonProperties.ZK_SIGNATURE).getAsString();
        String merkleTreeRoot = proofJson.get(HcsVcDocumentZeroKnowledgeJsonProperties.MERKLE_TREE_ROOT).getAsString();
        ZeroKnowledgeSignature<DrivingLicense> signature = new ZkSignature<>(zkSignature, merkleTreeRoot, new MerkleTreeFactoryImpl());
        document.setZeroKnowledgeSignature(signature);

        Ed25519CredentialProof proof = initializeProof(jsonDocument);
        document.setProof(proof);

        JsonObject credentialSchemaJson = jsonDocument.get("credentialSchema").getAsJsonObject();
        String id = credentialSchemaJson.get("id").getAsString();
        String type = credentialSchemaJson.get("type").getAsString();
        CredentialSchema credentialSchema = new CredentialSchema(id, type);
        document.setCredentialSchema(credentialSchema);

        return document;
    }

    private Ed25519CredentialProof initializeProof(JsonObject document) {
        String issuer = document.get(HcsVcDocumentJsonProperties.ISSUER).getAsString();
        JsonObject proofObject = document.get(HcsVcDocumentJsonProperties.PROOF).getAsJsonObject();

        Ed25519CredentialProof proof = new Ed25519CredentialProof(issuer);

        String type = proofObject.get("type").getAsString();
        proof.setType(type);

        String creator = proofObject.get("creator").getAsString();
        proof.setCreator(creator);

        String created = proofObject.get("created").getAsString();
        proof.setCreated(Instant.parse(created));

        String proofOfPurpose = proofObject.get("proofPurpose").getAsString();
        proof.setProofPurpose(proofOfPurpose);

        String verificationMethod = proofObject.get("verificationMethod").getAsString();
        proof.setVerificationMethod(verificationMethod);

        String jws = proofObject.get("jws").getAsString();
        proof.setJws(jws);

        return proof;
    }

    private List<DrivingLicense> translateToDriverLicense(JsonElement jsonElement) {
        List<DrivingLicense> drivingLicenses = new ArrayList<>();

        // TODO: try using "gson.fromJson(element, BirthDate.class)" instead of custom serializer

        for (JsonElement element : jsonElement.getAsJsonArray()) {
            JsonObject drivingLicenseField = element.getAsJsonObject();

            String did = drivingLicenseField.get("id").getAsString();
            String firstName = drivingLicenseField.get("firstName").getAsString();
            String lastName = drivingLicenseField.get("lastName").getAsString();
            List<String> drivingLicenseCategories = getJsonElementAsList(drivingLicenseField.get("drivingLicenseCategories"));
            BirthDate birthDate = getJsonElementAsBirthDate(drivingLicenseField.get("birthDate"));

            drivingLicenses.add(new DrivingLicense(did, firstName, lastName, drivingLicenseCategories, birthDate));
        }

        return drivingLicenses;
    }

    @Override
    protected DrivingLicenseZeroKnowledgeDocument initializeNewBlankDocument() {
        return new DrivingLicenseZeroKnowledgeDocument();
    }
}
