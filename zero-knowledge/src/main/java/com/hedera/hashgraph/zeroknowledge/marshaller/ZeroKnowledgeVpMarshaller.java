package com.hedera.hashgraph.zeroknowledge.marshaller;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hedera.hashgraph.identity.utils.JsonUtils;
import com.hedera.hashgraph.zeroknowledge.vp.HcsVpDocumentBase;
import com.hedera.hashgraph.zeroknowledge.vp.HcsVpDocumentJsonProperties;
import com.hedera.hashgraph.zeroknowledge.vp.VerifiableCredentialBase;
import org.threeten.bp.Instant;

import java.util.LinkedHashMap;
import java.util.List;

import static com.hedera.hashgraph.identity.utils.JsonUtils.getJsonElementAsList;

/**
 * A base class used to marshall and unmarshall a verifiable presentation to a formatted json string, based on the w3c standard.
 *
 * @param <T> The type of the verifiable presentation document.
 */
public abstract class ZeroKnowledgeVpMarshaller<T extends HcsVpDocumentBase<? extends VerifiableCredentialBase>> implements VpMarshaller<T> {
    private static final String[] JSON_PROPERTIES_ORDER = {"@context", "id", "type", "issuanceDate", "holder"};
    protected static final Gson gson = JsonUtils.getGson();

    @Override
    public String fromDocumentToString(T vcDocument) {
        // First turn to normal JSON
        JsonObject root = gson.toJsonTree(vcDocument).getAsJsonObject();
        // Then put JSON properties in ordered map
        LinkedHashMap<String, JsonElement> map = new LinkedHashMap<>();
        for (String property : JSON_PROPERTIES_ORDER) {
            if (root.has(property)) {
                map.put(property, root.get(property));
            }
        }
        // Turn map to JSON
        return gson.toJson(map);
    }

    @Override
    public T fromStringToDocument(String stringDocument) {
        JsonObject jsonDocument = gson.fromJson(stringDocument, JsonObject.class);
        return fromJsonToDocument(jsonDocument);
    }

    /**
     * From json string format to concrete document converter.
     *
     * @param jsonDocument The document represented as a json object.
     * @return The internal representation of a verifiable presentation.
     */
    protected T fromJsonToDocument(JsonObject jsonDocument) {
        T decodedDocument = initializeNewBlankDocument();

        String docId = jsonDocument.get(HcsVpDocumentJsonProperties.ID).getAsString();
        decodedDocument.setId(docId);

        List<String> docTypes = getJsonElementAsList(jsonDocument.get(HcsVpDocumentJsonProperties.TYPE));
        decodedDocument.setType(docTypes);

        String holder = jsonDocument.get(HcsVpDocumentJsonProperties.HOLDER).getAsString();
        decodedDocument.setHolder(holder);

        String issuanceDate = jsonDocument.get(HcsVpDocumentJsonProperties.ISSUANCE_DATE).getAsString();
        decodedDocument.setIssuanceDate(Instant.parse(issuanceDate));

        List<String> docContext = getJsonElementAsList(jsonDocument.get(HcsVpDocumentJsonProperties.CONTEXT));
        decodedDocument.setContext(docContext);

        return decodedDocument;
    }

    /**
     * An abstract method that the concrete class has to implement to return its blank document type.
     *
     * @return A concrete blank document to be filled.
     */
    protected abstract T initializeNewBlankDocument();
}
