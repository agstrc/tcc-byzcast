package dev.agst.byzcast.replica;

import bftsmart.tom.MessageContext;
import bftsmart.tom.server.defaultservices.DefaultRecoverable;
import dev.agst.byzcast.Logger;
import dev.agst.byzcast.Logger.Attr;
import dev.agst.byzcast.Serializer;
import dev.agst.byzcast.SerializingException;
import dev.agst.byzcast.message.Request;
import dev.agst.byzcast.message.Response;
import dev.agst.byzcast.message.TopologyUpdateRequest;
import dev.agst.byzcast.topology.TopologyManager;
import java.util.ArrayList;
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
 *   <li>Handling topology update requests via BFT-SMaRT consensus.
 *   <li>Ensuring that requests and responses are well integrated with {@code ReplicaReplier} for
 *       reliable communication.
 * </ul>
 *
 * @see dev.agst.byzcast.replica.RequestHandler
 * @see dev.agst.byzcast.replica.ReplicaReplier
 */
public class ReplicaNode extends DefaultRecoverable {
  private final Logger logger;
  private final RequestHandler handler;
  private final TopologyManager topologyManager;

  private ReplicaState state;

  public ReplicaNode(
      Logger logger, RequestHandler handler, ReplicaState state, TopologyManager topologyManager) {
    this.logger = logger;
    this.handler = handler;
    this.state = state;
    this.topologyManager = topologyManager;
  }

  /**
   * A factory method for creating instances of {@code ReplicaNode}. This method is used to ensure
   * that all necessary components are properly initialized before the instance is created.
   */
  public static ReplicaNodeBuilderFactory.LoggerConfigurator builder() {
    return new ReplicaNodeBuilderFactory.Builder();
  }

  @Override
  public byte[][] appExecuteBatch(byte[][] cmds, MessageContext[] ctxs) {
    return Arrays.stream(cmds).map(this::appExecuteSingle).toArray(byte[][]::new);
  }

  private byte[] appExecuteSingle(byte[] cmd) {
    // First, try to deserialize as TopologyUpdateRequest
    try {
      var topologyUpdate = Serializer.fromBytes(cmd, TopologyUpdateRequest.class);
      return handleTopologyUpdate(topologyUpdate);
    } catch (SerializingException e) {
      // Not a TopologyUpdateRequest, try as regular Request
    }

    // Try to deserialize as regular Request
    Request request;
    try {
      request = Serializer.fromBytes(cmd, Request.class);
    } catch (Exception e) {
      logger.error("Failed to deserialize request", e);
      var response = new Response("INVALID_PAYLOAD", new ArrayList<>());
      var reply = new ReplicaReply.Raw(Serializer.toBytes(response));

      return Serializer.toBytes(reply);
    }

    try {
      ReplicaReply reply = this.handler.handle(request, state);
      return Serializer.toBytes(reply);
    } catch (Exception e) {
      logger.error("Failed to handle request", e, new Attr("RID", request.id()));

      var response = new Response("INTERNAL_ERROR", new ArrayList<>());
      var errorReply = new ReplicaReply.Raw(Serializer.toBytes(response));
      return Serializer.toBytes(errorReply);
    }
  }

  /**
   * Handles a topology update request by updating the TopologyManager.
   *
   * @param updateRequest The topology update request.
   * @return The serialized ReplicaReply containing the response.
   */
  private byte[] handleTopologyUpdate(TopologyUpdateRequest updateRequest) {
    logger.info("Processing topology update request", new Attr("requestId", updateRequest.id()));

    try {
      topologyManager.updateTopology(updateRequest.topology());
      logger.info("Topology updated successfully", new Attr("requestId", updateRequest.id()));

      var response = new Response("TOPOLOGY_UPDATED", new ArrayList<>());
      var reply = new ReplicaReply.Raw(Serializer.toBytes(response));
      return Serializer.toBytes(reply);
    } catch (Exception e) {
      logger.error("Failed to update topology", e, new Attr("requestId", updateRequest.id()));

      var response = new Response("TOPOLOGY_UPDATE_FAILED: " + e.getMessage(), new ArrayList<>());
      var reply = new ReplicaReply.Raw(Serializer.toBytes(response));
      return Serializer.toBytes(reply);
    }
  }

  @Override
  public byte[] appExecuteUnordered(byte[] cmd, MessageContext ctx) {
    var response = new Response("UNSUPPORTED_OPERATION", new ArrayList<>());
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
