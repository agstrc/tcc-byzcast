package dev.agst.byzcast.server;

import bftsmart.tom.MessageContext;
import bftsmart.tom.server.defaultservices.DefaultRecoverable;
import dev.agst.byzcast.Serializer;
import dev.agst.byzcast.SerializingException;
import dev.agst.byzcast.message.Request;
import dev.agst.byzcast.message.Response.AggregatedResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public class ServerNode extends DefaultRecoverable {
  private final RequestHandler handler;

  private ServerState state;

  public ServerNode(RequestHandler handler, ServerState state) {
    this.handler = handler;
    this.state = state;
  }

  @Override
  public byte[][] appExecuteBatch(byte[][] cmds, MessageContext[] ctxs) {

    var requests =
        Arrays.stream(cmds)
            .map(
                cmd -> {
                  Request request;
                  try {
                    request = Serializer.fromBytes(cmd, Request.class);
                  } catch (SerializingException e) {
                    // TODO: improve error handling
                    throw new RuntimeException("Could not deserialize request", e);
                  }
                  return request;
                })
            .collect(Collectors.toList());

    var replies = handler.handle(requests, state);
    System.out.println("RETURN REPLIES");
    System.out.println(replies);
    var returnVal = replies.stream().map(reply -> Serializer.toBytes(reply)).toArray(byte[][]::new);
    return returnVal;
  }

  @Override
  public byte[] appExecuteUnordered(byte[] cmd, MessageContext ctx) {
    var response = new AggregatedResponse("NOT_SUPPORTED", new ArrayList<>());
    return Serializer.toBytes(response);
  }

  @Override
  public byte[] getSnapshot() {
    return Serializer.toBytes(state);
  }

  @Override
  public void installSnapshot(byte[] snapshot) {
    try {
      state = Serializer.fromBytes(snapshot, ServerState.class);
    } catch (SerializingException e) {
      throw new RuntimeException("Could not deserialize state", e);
    }
  }
}
