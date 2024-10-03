package dev.agst.byzcast.server;

import bftsmart.tom.MessageContext;
import bftsmart.tom.server.defaultservices.DefaultRecoverable;
import dev.agst.byzcast.Logger;
import dev.agst.byzcast.Serializer;
import dev.agst.byzcast.SerializingException;
import dev.agst.byzcast.message.Request;
import dev.agst.byzcast.message.Response.AggregatedResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public class ServerNode extends DefaultRecoverable {
  private final Logger logger;

  private final RequestHandler requestHandler;

  private ServerState state;

  public ServerNode(Logger logger, RequestHandler requestHandler, ServerState state) {
    this.logger = logger;
    this.requestHandler = requestHandler;
    this.state = state;
  }

  @Override
  public byte[][] appExecuteBatch(byte[][] cmds, MessageContext[] ctxs) {
    var requests =
        Arrays.stream(cmds)
            .map(
                cmd -> {
                  try {
                    return Serializer.fromBytes(cmd, Request.class);
                  } catch (SerializingException e) {
                    logger.error("Failed to deserialize request", e);
                    throw new RuntimeException(e);
                  }
                })
            .collect(Collectors.toCollection(ArrayList::new));

    var replies = requestHandler.handle(requests, state);

    return replies.stream().map(reply -> Serializer.toBytes(reply)).toArray(byte[][]::new);
  }

  @Override
  public byte[] appExecuteUnordered(byte[] cmd, MessageContext ctx) {
    var response = new AggregatedResponse("NOT_IMPLEMENTED", null);
    return Serializer.toBytes(response);
  }

  @Override
  public byte[] getSnapshot() {
    return Serializer.toBytes(state);
  }

  @Override
  public void installSnapshot(byte[] data) {
    try {
      state = Serializer.fromBytes(data, ServerState.class);
    } catch (SerializingException e) {
      logger.error("Failed to deserialize snapshot", e);
      throw new RuntimeException(e);
    }
  }
}
