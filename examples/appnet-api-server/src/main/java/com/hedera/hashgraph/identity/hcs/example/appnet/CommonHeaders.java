package com.hedera.hashgraph.identity.hcs.example.appnet;

import com.hedera.hashgraph.identity.hcs.example.appnet.handlers.DemoHandler;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.http.MutableHeaders;

/**
 * A helper class that configures REST server's response headers.
 */
public class CommonHeaders implements Handler {

  @Override
  public void handle(final Context ctx) throws Exception {
    ctx.getResponse().noCompress();
    ctx.getResponse().contentType("application/json");
    MutableHeaders headers = ctx.getResponse().getHeaders();
    headers.set("Access-Control-Allow-Origin", "*");
    headers.set("Access-Control-Allow-Headers", DemoHandler.HEADER_PRIVATE_KEY + ", Content-Type");
    headers.set("Access-Control-Allow-Methods", "*");
    ctx.next();
  }
}
