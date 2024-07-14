package dev.agst.byzcast.replica;

import bftsmart.tom.MessageContext;
import bftsmart.tom.server.defaultservices.DefaultRecoverable;
import dev.agst.byzcast.Logger;
import dev.agst.byzcast.Serializer;
import dev.agst.byzcast.message.Request;
import dev.agst.byzcast.message.Response;
import dev.agst.byzcast.topology.Topology;
import java.util.Arrays;

/**
 * Implements a node within the ByzCast system, acting as a controller for serializing and
 * deserializing requests and responses.
 *
 * <p>This class is responsible for:
 *
 * <ul>
 *   <li>Serializing requests to be processed by the system.
 *   <li>Deserializing responses for the clients.
 *   <li>Forwarding the logical handling of requests to the {@code RequestHandler} class.
 *   <li>Ensuring that requests and responses are well integrated with {@code ReplicaReplier} for
 *       reliable communication.
 * </ul>
 *
 * @see dev.agst.byzcast.replica.RequestHandler
 * @see dev.agst.byzcast.replica.ReplicaReplier
 */
public class ReplicaNode extends DefaultRecoverable {
  private final Logger logger;
  private final ReplicaInfo info;
  private final RequestHandler handler;

  private ReplicaState state;

  public ReplicaNode(int targetForwardCount, ReplicaInfo replicaInfo, Topology topology) {
    this.info = replicaInfo;

    this.state = new ReplicaState(targetForwardCount);
    this.handler = new RequestHandler(replicaInfo, topology);

    this.logger =
        new Logger()
            .with("groupID", replicaInfo.groupID())
            .with("serverID", replicaInfo.serverID());
  }

  @Override
  public byte[][] appExecuteBatch(byte[][] cmds, MessageContext[] ctxs) {
    return Arrays.stream(cmds).map(this::appExecuteSingle).toArray(byte[][]::new);
  }

  private byte[] appExecuteSingle(byte[] cmd) {
    Request request;

    try {
      request = Serializer.fromBytes(cmd, Request.class);
    } catch (Exception e) {
      logger.error("Failed to deserialize request", e);
      var response = new Response("INVALID_PAYLOAD", new int[] {this.info.groupID()});
      var reply = new ReplicaReply.Raw(Serializer.toBytes(response));

      return Serializer.toBytes(reply);
    }

    try {
      ReplicaReply reply = this.handler.handle(request, state);
      return Serializer.toBytes(reply);
    } catch (Exception e) {
      logger.with("requestID", request.id()).error("Failed to handle request", e);
      var response = new Response("INTERNAL_ERROR", new int[] {this.info.groupID()});
      var errorReply = new ReplicaReply.Raw(Serializer.toBytes(response));
      return Serializer.toBytes(errorReply);
    }
  }

  @Override
  public byte[] appExecuteUnordered(byte[] cmd, MessageContext ctx) {
    var response = new Response("UNSUPPORTED_OPERATION", new int[] {this.info.groupID()});
    var rawResponse = Serializer.toBytes(response);
    return Serializer.toBytes(new ReplicaReply.Raw(rawResponse));
  }

  @Override
  public byte[] getSnapshot() {
    return Serializer.toBytes(this.state);
  }

  @Override
  public void installSnapshot(byte[] state) {
    try {
      this.state = Serializer.fromBytes(state, ReplicaState.class);
    } catch (Exception e) {
      logger.error("Failed to deserialize snapshot", e);
      throw new RuntimeException(e);
    }
  }
}
