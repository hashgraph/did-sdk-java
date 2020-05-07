package com.hedera.hashgraph.identity.hcs.example.appnet.handlers;

import com.hedera.hashgraph.identity.hcs.HcsIdentityNetwork;
import com.hedera.hashgraph.identity.hcs.MessageEnvelope;
import com.hedera.hashgraph.identity.hcs.example.appnet.AppnetStorage;
import com.hedera.hashgraph.identity.hcs.example.appnet.dto.ErrorResponse;
import com.hedera.hashgraph.identity.hcs.example.appnet.dto.VerifiableCredentialStatus;
import com.hedera.hashgraph.identity.hcs.vc.HcsVcMessage;
import com.hedera.hashgraph.identity.hcs.vc.HcsVcOperation;
import com.hedera.hashgraph.identity.utils.JsonUtils;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.TransactionId;
import com.hedera.hashgraph.sdk.mirror.MirrorClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.handling.Context;
import ratpack.http.Status;
import ratpack.jackson.Jackson;

/**
 * Request handler for VC status operations.
 * This handler is an example implementation of appnet's REST API as defined in Verifiable Credentials specification for
 * Hedera DID Method Specification.
 */
public class VcHandler extends AppnetHandler {
  private static Logger log = LoggerFactory.getLogger(VcHandler.class);
  private static final String PATH_PARAM_CREDENTIAL_HASH = "credentialHash";

  /**
   * Instantiates the handler.
   *
   * @param identityNetwork The Hedera Identity network.
   * @param storage         The appnet's local storage.
   */
  public VcHandler(final HcsIdentityNetwork identityNetwork, final AppnetStorage storage) {
    super(identityNetwork, storage);
  }

  /**
   * Constructs a new VC topic message for VC ISSUE operation.
   * The credential hash is provided as path parameter.
   * The unsigned message is written into the response body.
   * The appnet may decide to encrypt it. This example works in a plain mode.
   *
   * @param ctx The HTTP context.
   */
  public void issue(final Context ctx) {
    buildVcMessage(ctx, HcsVcOperation.ISSUE);
  }

  /**
   * Constructs a new VC topic message for VC SUSPEND operation.
   * The credential hash is provided as path parameter.
   * The unsigned message is written into the response body.
   * The appnet may decide to encrypt it. This example works in a plain mode.
   *
   * @param ctx The HTTP context.
   */
  public void suspend(final Context ctx) {
    buildVcMessage(ctx, HcsVcOperation.SUSPEND);
  }

  /**
   * Constructs a new VC topic message for VC RESUME operation.
   * The credential hash is provided as path parameter.
   * The unsigned message is written into the response body.
   * The appnet may decide to encrypt it. This example works in a plain mode.
   *
   * @param ctx The HTTP context.
   */
  public void resume(final Context ctx) {
    buildVcMessage(ctx, HcsVcOperation.RESUME);
  }

  /**
   * Constructs a new VC topic message for VC REVOKE operation.
   * The credential hash is provided as path parameter.
   * The unsigned message is written into the response body.
   * The appnet may decide to encrypt it. This example works in a plain mode.
   *
   * @param ctx The HTTP context.
   */
  public void revoke(final Context ctx) {
    buildVcMessage(ctx, HcsVcOperation.REVOKE);
  }

  /**
   * Submits a built and signed VC status change message into the HCS VC topic.
   * Returns 200 OK when submission was successful, without waiting for the consensus result.
   *
   * @param ctx          The HTTP context.
   * @param client       The Hedera node client.
   * @param mirrorClient The Hedera mirror node client.
   */
  public void submit(final Context ctx, final Client client, final MirrorClient mirrorClient) {
    ctx.getRequest().getBody().then(data -> submitVcMessage(ctx, data.getText(), client, mirrorClient));
  }

  /**
   * Resolves the status of a verifiable credential against appent's local storage.
   * Returns 200 OK when resolution was successful and writes the latest valid
   * message received from the mirror node into the response body.
   * Returns 404 NOT FOUND if resolution was unsuccessful.
   *
   * @param ctx The HTTP context.
   */
  public void resolveVcStatus(final Context ctx) {
    try {
      String credentialHash = ctx.getPathTokens().get(PATH_PARAM_CREDENTIAL_HASH);

      String result = "";
      VerifiableCredentialStatus status = storage.resolveVcStatus(credentialHash);
      if (status == null) {
        ctx.getResponse().status(Status.NOT_FOUND);
      } else {
        result = JsonUtils.getGson().toJson(status);
      }

      ctx.render(result);
    } catch (Exception e) {
      ctx.getResponse().status(Status.BAD_REQUEST);
      ctx.render(Jackson.json(new ErrorResponse("VC status resolution failed", e)));
    }
  }

  /**
   * Constructs a new VC topic message for the given VC operation and credential hash.
   * The unsigned message is written into the response body.
   *
   * @param ctx       The HTTP context.
   * @param operation The operation on VC.
   */
  private void buildVcMessage(final Context ctx, final HcsVcOperation operation) {
    String credentialHash = ctx.getPathTokens().get(PATH_PARAM_CREDENTIAL_HASH);
    try {
      MessageEnvelope<HcsVcMessage> message = HcsVcMessage.fromCredentialHash(credentialHash, operation);
      ctx.render(message.toJson());
    } catch (Exception e) {
      ctx.getResponse().status(Status.BAD_REQUEST);
      ctx.render(Jackson.json(new ErrorResponse(e)));
    }
  }

  /**
   * Constructs a VC message from the given request body JSON.
   * The message is expected to be signed.
   * Then submits this signed message into the HCS VC topic.
   * Returns 202 ACCEPTED with empty body when submission was successful, without waiting for the consensus result.
   *
   * @param ctx          The HTTP context.
   * @param json         The VC topic message as JSON string.
   * @param client       The Hedera node client.
   * @param mirrorClient The Hedera mirror node client.
   */
  private void submitVcMessage(final Context ctx, final String json, final Client client,
      final MirrorClient mirrorClient) {
    final Hbar fee = new Hbar(2);

    try {
      MessageEnvelope<HcsVcMessage> message = MessageEnvelope.fromJson(json, HcsVcMessage.class);
      TransactionId txId = identityNetwork.createVcTransaction(message, null)
          .buildAndSignTransaction(tx -> tx.setMaxTransactionFee(fee).build(client))
          .onError(e -> log.error("Error while sending VC message transaction:", e))
          .execute(client, mirrorClient);

      log.info("VC message transaction submitted: " + txId.toString());
      ctx.getResponse().status(Status.ACCEPTED);
      ctx.render("");
    } catch (Exception e) {
      ctx.getResponse().status(Status.BAD_REQUEST);
      ctx.render(Jackson.json(new ErrorResponse(e)));
    }
  }

}
