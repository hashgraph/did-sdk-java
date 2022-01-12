package com.hedera.hashgraph.zeroknowledge.vc;

import com.hedera.hashgraph.identity.hcs.vc.CredentialSubject;
import com.hedera.hashgraph.identity.hcs.vc.HcsVcDocumentJsonProperties;
import com.hedera.hashgraph.identity.hcs.vc.Issuer;
import com.hedera.hashgraph.zeroknowledge.vp.proof.ZeroKnowledgeSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.threeten.bp.Instant;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HcsVcDocumentZeroKnowledgeTest {
    private static final String VC_DOC_ID = "fake-docId";

    private static final List<String> VC_DOC_TYPES = Collections.emptyList();

    @Mock
    private Issuer issuer;

    private static final Instant VC_DOC_ISSUANCE_DATE = Instant.now();

    @Mock
    private ZeroKnowledgeSignature<CredentialSubject> zeroKnowledgeSignature;
    private static final String FAKE_ZK_SIGNATURE = "fake-zkSignature";

    private HcsVcDocumentZeroKnowledge<CredentialSubject> zeroKnowledgeDocument;

    @BeforeEach
    public void init() {
        zeroKnowledgeDocument = new HcsVcDocumentZeroKnowledge<>();
        zeroKnowledgeDocument.setId(VC_DOC_ID);
        zeroKnowledgeDocument.setType(VC_DOC_TYPES);
        zeroKnowledgeDocument.setIssuer(issuer);
        zeroKnowledgeDocument.setIssuanceDate(VC_DOC_ISSUANCE_DATE);
        zeroKnowledgeDocument.setZeroKnowledgeSignature(zeroKnowledgeSignature);
        when(zeroKnowledgeSignature.getSignature()).thenReturn(FAKE_ZK_SIGNATURE);
    }

    @Test
    public void createAZeroKnowledgeVcDocument_TheZKSignatureIsAddedAsCredentialHashField() {
        // Arrange

        // Act
        Map<String, Object> map = zeroKnowledgeDocument.getCustomHashableFieldsHook();

        // Assert
        assertEquals(map.size(), 1);
        assertNotNull(map.getOrDefault(HcsVcDocumentZeroKnowledgeJsonProperties.ZK_SIGNATURE, null));
    }
}