package dev.agst.byzcast.server;

import bftsmart.tom.MessageContext;
import bftsmart.tom.server.defaultservices.DefaultRecoverable;
import dev.agst.byzcast.Logger;
import dev.agst.byzcast.Serializer;
import dev.agst.byzcast.v2.message.Request;
import java.util.ArrayList;

public class ServerReplica extends DefaultRecoverable {
  private final Logger logger;
  private final ServerHandler handler;

  private ServerState state;

  public ServerReplica(Logger logger, ServerHandler handler, ServerState state) {
    this.logger = logger;
    this.handler = handler;
    this.state = state;
  }

  @Override
  public byte[][] appExecuteBatch(byte[][] cmds, MessageContext[] ctxs) {
    var requests = new ArrayList<Request>(cmds.length);

    for (var cmd : cmds) {
      try {
        var req = (Request) Serializer.fromBytes(cmd, Request.class);
        requests.add(req);
      } catch (Exception e) {
        logger.error("Failed to deserialize request", e);
        throw new RuntimeException(e);
      }
    }

    var replies = handler.handle(requests, state);
    return replies.stream().map(Serializer::toBytes).toArray(byte[][]::new);
  }

  @Override
  public byte[] appExecuteUnordered(byte[] arg0, MessageContext arg1) {
    return "NOT_IMPLEMENTED".getBytes();
  }

  @Override
  public byte[] getSnapshot() {
    return Serializer.toBytes(state);
  }

  @Override
  public void installSnapshot(byte[] bytes) {
    try {
      state = (ServerState) Serializer.fromBytes(bytes, ServerState.class);
    } catch (Exception e) {
      logger.error("Failed to deserialize snapshot", e);
      throw new RuntimeException(e);
    }
  }
}
