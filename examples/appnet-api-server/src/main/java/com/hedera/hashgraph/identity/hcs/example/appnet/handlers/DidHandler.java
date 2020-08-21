package com.hedera.hashgraph.identity.hcs.example.appnet.handlers;

import com.google.gson.JsonSyntaxException;
import com.hedera.hashgraph.identity.DidMethodOperation;
import com.hedera.hashgraph.identity.hcs.HcsIdentityNetwork;
import com.hedera.hashgraph.identity.hcs.MessageEnvelope;
import com.hedera.hashgraph.identity.hcs.did.HcsDidMessage;
import com.hedera.hashgraph.identity.hcs.example.appnet.AppnetStorage;
import com.hedera.hashgraph.identity.hcs.example.appnet.dto.DidResolutionRequest;
import com.hedera.hashgraph.identity.hcs.example.appnet.dto.ErrorResponse;
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
 * Request handler for DID method operations.
 * This handler is an example implementation of appnet's REST API as defined in Hedera DID Method Specification.
 */
public class DidHandler extends AppnetHandler {
  private static Logger log = LoggerFactory.getLogger(DidHandler.class);

  /**
   * Instantiates the handler.
   *
   * @param identityNetwork The Hedera Identity network.
   * @param storage         The appnet's local storage.
   */
  public DidHandler(final HcsIdentityNetwork identityNetwork, final AppnetStorage storage) {
    super(identityNetwork, storage);
  }

  /**
   * Constructs a new DID topic message for DID CREATE operation.
   * The DID document is provided in the request body.
   * The unsigned message is written into the response body.
   * The appnet may decide to encrypt it. This example works in a plain mode.
   *
   * @param ctx The HTTP context.
   */
  public void create(final Context ctx) {
    ctx.getRequest().getBody().then(data -> buildDidMessage(ctx, data.getText(), DidMethodOperation.CREATE));
  }

  /**
   * Constructs a new DID topic message for DID UPDATE operation.
   * The DID document is provided in the request body.
   * The unsigned message is written into the response body.
   * The appnet may decide to encrypt it. This example works in a plain mode.
   *
   * @param ctx The HTTP context.
   */
  public void update(final Context ctx) {
    ctx.getRequest().getBody().then(data -> buildDidMessage(ctx, data.getText(), DidMethodOperation.UPDATE));
  }

  /**
   * Constructs a new DID topic message for DID DELETE operation.
   * The DID document is provided in the request body.
   * The unsigned message is written into the response body.
   * The appnet may decide to encrypt it. This example works in a plain mode.
   *
   * @param ctx The HTTP context.
   */
  public void delete(final Context ctx) {
    ctx.getRequest().getBody().then(data -> buildDidMessage(ctx, data.getText(), DidMethodOperation.DELETE));
  }

  /**
   * Submits a built and signed CREATE, UPDATE or DELETE message into the HCS DID topic.
   * Returns 200 OK when submission was successful, without waiting for the consensus result.
   *
   * @param ctx          The HTTP context.
   * @param client       The Hedera node client.
   * @param mirrorClient The Hedera mirror node client.
   */
  public void submit(final Context ctx, final Client client, final MirrorClient mirrorClient) {
    ctx.getRequest().getBody().then(data -> submitDidMessage(ctx, data.getText(), client, mirrorClient));
  }

  /**
   * Resolves the DID provided in the request body into the DID document.
   * Returns HTTP 404 if DID could not be resolved (not found or deleted).
   * The DID document is returned in a JSON response body.
   *
   * @param ctx The HTTP context.
   */
  public void resolve(final Context ctx) {
    ctx.getRequest().getBody().then(data -> resolveDid(ctx, data.getText()));
  }

  /**
   * Constructs a new DID topic message for the given input data.
   * The unsigned message is written into the response body.
   *
   * @param ctx       The HTTP context.
   * @param json      The DID document as JSON string.
   * @param operation The DID Method Operation.
   */
  private void buildDidMessage(final Context ctx, final String json, final DidMethodOperation operation) {
    try {
      MessageEnvelope<HcsDidMessage> message = HcsDidMessage.fromDidDocumentJson(json, operation);
      ctx.render(message.toJson());
    } catch (Exception e) {
      ctx.getResponse().status(Status.BAD_REQUEST);
      ctx.render(Jackson.json(new ErrorResponse(e)));
    }
  }

  /**
   * Extracts the DID to be resolved from the request body.
   * Then tries to resolve it against appnet's local storage.
   * Responds with HTTP 200 OK and DID document in response body if resolution succeded.
   * Otherwise responds with HTTP 404 NOT FOUND and empty body.
   *
   * @param ctx  The HTTP context.
   * @param json The request body as JSON string.
   */
  private void resolveDid(final Context ctx, final String json) {
    try {
      DidResolutionRequest request = JsonUtils.getGson().fromJson(json, DidResolutionRequest.class);

      String result = "";
      HcsDidMessage didMessage = storage.resolveDid(request.getDid());
      if (didMessage == null || DidMethodOperation.DELETE.equals(didMessage.getOperation())) {
        ctx.getResponse().status(Status.NOT_FOUND);
      } else {
        result = didMessage.getDidDocument();
      }

      ctx.render(result);
    } catch (JsonSyntaxException ex) {
      ctx.getResponse().status(Status.BAD_REQUEST);
      ctx.render(Jackson.json(new ErrorResponse("Invalid request body", ex)));
    } catch (Exception e) {
      ctx.getResponse().status(Status.BAD_REQUEST);
      ctx.render(Jackson.json(new ErrorResponse("Resolution failed", e)));
    }
  }

  /**
   * Constructs a DID message from the given request body JSON.
   * The message is expected to be signed.
   * Then submits this signed CREATE, UPDATE or DELETE message into the HCS DID topic.
   * Returns 202 ACCEPTED with empty body when submission was successful, without waiting for the consensus result.
   *
   * @param ctx          The HTTP context.
   * @param json         The DID topic message as JSON string.
   * @param client       The Hedera node client.
   * @param mirrorClient The Hedera mirror node client.
   */
  private void submitDidMessage(final Context ctx, final String json, final Client client,
      final MirrorClient mirrorClient) {
    final Hbar fee = new Hbar(2);

    try {
      MessageEnvelope<HcsDidMessage> message = MessageEnvelope.fromJson(json, HcsDidMessage.class);
      TransactionId txId = identityNetwork.createDidTransaction(message)
          .buildAndSignTransaction(tx -> tx.setMaxTransactionFee(fee).build(client))
          .onError(e -> log.error("Error while sending DID message transaction:", e))
          .execute(client, mirrorClient);

      log.info("DID message transaction submitted: " + txId.toString());
      ctx.getResponse().status(Status.ACCEPTED);
      ctx.render("");
    } catch (Exception e) {
      ctx.getResponse().status(Status.BAD_REQUEST);
      ctx.render(Jackson.json(new ErrorResponse(e)));
    }
  }
}
