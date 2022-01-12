package com.hedera.hashgraph.identity.hcs.example.appnet;

import com.hedera.hashgraph.identity.hcs.example.appnet.agecircuit.utils.ProvingSystemUtils;
import io.github.cdimascio.dotenv.Dotenv;
import io.horizen.common.librustsidechains.InitializationException;
import io.horizenlabs.agecircuit.AgeCircuitProof;
import io.horizenlabs.agecircuit.AgeCircuitProofException;
import io.horizenlabs.provingsystemnative.ProvingSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This entrypoint is used to generate the proving and verification keys; the former used by the circuit to generate
 * a zero knowledge proof, the latter to verify it.
 * This operation is executed only once, since both the prover and verifier are going to use the same keys to generate/verify
 * the proof with the same circuit.
 * In a real environment, both the keys will be created once per circuit and hosted in a public repo.
 * Whoever wants to generate or verify a proof will download them locally.
 */
public class ZeroKnowledgeKeyGenerator {
    private static final Logger log = LoggerFactory.getLogger(ZeroKnowledgeKeyGenerator.class);

    public static void main(String[] args) throws InitializationException, AgeCircuitProofException {
        ProvingSystemUtils.generateDLogKeys();
        Dotenv dotenv = Dotenv.configure().load();

        String provingKeyPath = dotenv.get("PROVING_KEY_PATH");
        String verificationKeyPath = dotenv.get("VERIFICATION_KEY_PATH");

        log.info("Initializing proving and verification key path...");
        AgeCircuitProof.setup(provingKeyPath, verificationKeyPath);
        log.info("Done creating proving and verification keys!");
    }
}
