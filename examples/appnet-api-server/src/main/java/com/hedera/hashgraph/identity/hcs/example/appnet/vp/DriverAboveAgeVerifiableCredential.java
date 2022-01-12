package com.hedera.hashgraph.identity.hcs.example.appnet.vp;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.hedera.hashgraph.identity.hcs.example.appnet.vc.CredentialSchema;
import com.hedera.hashgraph.identity.hcs.vc.Issuer;
import com.hedera.hashgraph.identity.utils.JsonUtils;
import com.hedera.hashgraph.zeroknowledge.vp.VerifiableCredential;
import com.hedera.hashgraph.zeroknowledge.vp.VerifiableCredentialJsonProperties;
import org.threeten.bp.Instant;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DriverAboveAgeVerifiableCredential extends VerifiableCredential {
    private static final String[] JSON_PROPERTIES_ORDER = {"@context", "id", "type", "credentialSchema", "issuer",
            "issuanceDate", "credentialSubject", "proof"};

    @Expose
    @SerializedName(VerifiableCredentialJsonProperties.CREDENTIAL_SCHEMA)
    private CredentialSchema credentialSchema;

    @Expose
    @SerializedName(VerifiableCredentialJsonProperties.CREDENTIAL_SUBJECT)
    protected Map<String, String> credentialSubject;

    public DriverAboveAgeVerifiableCredential() {
        super();
        this.credentialSubject = new HashMap<>();
    }

    public List<String> getType() {
        return this.type;
    }

    public CredentialSchema getCredentialSchema() {
        return this.credentialSchema;
    }

    public Issuer getIssuer() {
        return this.issuer;
    }

    public Instant getIssuanceDate() {
        return this.issuanceDate;
    }

    public Map<String, String> getCredentialSubject() {
        return this.credentialSubject;
    }

    public void setContext(List<String> context) {
        this.context = context;
    }

    public void setType(List<String> type) {
        this.type = type;
    }

    public void setCredentialSchema(CredentialSchema credentialSchema) {
        this.credentialSchema = credentialSchema;
    }

    public void setIssuer(Issuer issuer) {
        this.issuer = issuer;
    }

    public void setIssuanceDate(Instant issuanceDate) {
        this.issuanceDate = issuanceDate;
    }

    public void setCredentialSubject(Map<String, String> newCredentialSubject) {
        this.credentialSubject = newCredentialSubject;
    }

    public void addCredentialSubjectClaim(String name, String value) {
        this.credentialSubject.put(name, value);
    }

    public JsonElement toNormalizedJsonElement() {
        Gson gson = JsonUtils.getGson();

        // First turn to normal JSON
        JsonObject root = gson.toJsonTree(this).getAsJsonObject();
        // Then put JSON properties in ordered map
        LinkedHashMap<String, JsonElement> map = new LinkedHashMap<>();

        for (String property : JSON_PROPERTIES_ORDER) {
            map.put(property, root.get(property));
        }
        // Turn map to JSON
        return gson.toJsonTree(map);
    }

    public void setId(String id) {
        this.id = id;
    }
}
