package com.hedera.hashgraph.identity.hcs.example.appnet.marshaller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hedera.hashgraph.identity.hcs.example.appnet.vc.CredentialSchema;
import com.hedera.hashgraph.identity.hcs.example.appnet.vp.DriverAboveAgePresentation;
import com.hedera.hashgraph.identity.hcs.example.appnet.vp.DriverAboveAgeVerifiableCredential;
import com.hedera.hashgraph.identity.hcs.vc.HcsVcDocumentJsonProperties;
import com.hedera.hashgraph.identity.hcs.vc.Issuer;
import com.hedera.hashgraph.identity.utils.JsonUtils;
import com.hedera.hashgraph.zeroknowledge.marshaller.ZeroKnowledgeVpMarshaller;
import com.hedera.hashgraph.zeroknowledge.vc.HcsVcDocumentZeroKnowledgeJsonProperties;
import com.hedera.hashgraph.zeroknowledge.vp.proof.PresentationProof;
import com.hedera.hashgraph.zeroknowledge.vp.proof.ZkSnarkPresentationProof;
import com.hedera.hashgraph.zeroknowledge.vp.HcsVpDocumentJsonProperties;
import org.threeten.bp.Instant;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static com.hedera.hashgraph.identity.utils.JsonUtils.getJsonElementAsList;

public class DriverAboveAgeVpMarshaller extends ZeroKnowledgeVpMarshaller<DriverAboveAgePresentation> {
    protected static final Gson gson = JsonUtils.getGson();

    private static final String[] JSON_PROPERTIES_ORDER = {"@context", "id", "type", "issuanceDate",
            "holder", "verifiableCredential"};

    @Override
    public String fromDocumentToString(DriverAboveAgePresentation vpDocument) {
        Gson gson = JsonUtils.getGson();

        // First turn to normal JSON
        JsonObject root = gson.toJsonTree(vpDocument).getAsJsonObject();
        // Then put JSON properties in ordered map
        LinkedHashMap<String, JsonElement> map = new LinkedHashMap<>();

        JsonArray verifiableCredentialArray = new JsonArray();
        for (String property : JSON_PROPERTIES_ORDER) {
            if (property.equals("verifiableCredential")) {
                for (DriverAboveAgeVerifiableCredential vc : vpDocument.getVerifiableCredential()) {
                    verifiableCredentialArray.add(vc.toNormalizedJsonElement());
                }
                map.put(property, verifiableCredentialArray);
            } else {
                map.put(property, root.get(property));
            }
        }
        // Turn map to JSON
        return gson.toJson(map);
    }

    @Override
    public DriverAboveAgePresentation fromStringToDocument(String stringDocument) {
        JsonObject jsonDocument = gson.fromJson(stringDocument, JsonObject.class);
        return fromJsonToDocument(jsonDocument);
    }

    protected DriverAboveAgePresentation fromJsonToDocument(JsonObject jsonDocument) {
        DriverAboveAgePresentation presentation = super.fromJsonToDocument(jsonDocument);

        List<DriverAboveAgeVerifiableCredential> verifiableCredentials = parseVerifiableCredentials(jsonDocument.get(HcsVpDocumentJsonProperties.VERIFIABLE_CREDENTIAL));
        presentation.setVerifiableCredential(verifiableCredentials);

        return presentation;
    }

    @Override
    protected DriverAboveAgePresentation initializeNewBlankDocument() {
        return new DriverAboveAgePresentation();
    }

    private List<DriverAboveAgeVerifiableCredential> parseVerifiableCredentials(JsonElement jsonElement) {
        List<DriverAboveAgeVerifiableCredential> verifiableCredentials = new ArrayList<>();

        for (JsonElement element : jsonElement.getAsJsonArray()) {
            JsonObject currElement = element.getAsJsonObject();
            DriverAboveAgeVerifiableCredential credential = new DriverAboveAgeVerifiableCredential();

            List<String> context = getJsonElementAsList(currElement.get(HcsVcDocumentJsonProperties.CONTEXT));
            credential.setContext(context);

            String id = currElement.get(HcsVcDocumentJsonProperties.ID).getAsString();
            credential.setId(id);

            List<String> types = getJsonElementAsList(currElement.get(HcsVcDocumentJsonProperties.TYPE));
            credential.setType(types);

            JsonObject credentialSchemaJson = currElement.get("credentialSchema").getAsJsonObject();
            String credentialSchemaId = credentialSchemaJson.get("id").getAsString();
            String credentialSchemaType = credentialSchemaJson.get("type").getAsString();
            CredentialSchema credentialSchema = new CredentialSchema(credentialSchemaId, credentialSchemaType);
            credential.setCredentialSchema(credentialSchema);

            String issuer = currElement.get(HcsVcDocumentJsonProperties.ISSUER).getAsString();
            credential.setIssuer(new Issuer(issuer));

            String issuanceDate = currElement.get(HcsVcDocumentJsonProperties.ISSUANCE_DATE).getAsString();
            credential.setIssuanceDate(Instant.parse(issuanceDate));

            JsonObject proofJson = currElement.get(HcsVcDocumentJsonProperties.PROOF).getAsJsonObject();
            String signature = proofJson.get(HcsVcDocumentZeroKnowledgeJsonProperties.ZK_SIGNATURE).getAsString();
            String snarkProof = proofJson.get("snarkProof").getAsString();
            PresentationProof proof = new ZkSnarkPresentationProof(signature, snarkProof);
            credential.setProof(proof);

            verifiableCredentials.add(credential);
        }

        return verifiableCredentials;
    }
}
