package dev.agst.byzcast.replica;

import bftsmart.tom.MessageContext;
import bftsmart.tom.server.defaultservices.DefaultRecoverable;
import dev.agst.byzcast.Logger;
import dev.agst.byzcast.Serializer;
import dev.agst.byzcast.SerializingException;
import dev.agst.byzcast.group.GroupMap;
import dev.agst.byzcast.group.GroupProxyRetriever;
import dev.agst.byzcast.message.Request;
import dev.agst.byzcast.message.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class ReplicaNode extends DefaultRecoverable {
  private final GroupMap groupMap;
  private final GroupProxyRetriever proxyRetriever;

  private final ReplicaInfo replicaInfo;
  private final Logger logger;

  private ReplicaState state;

  public ReplicaNode(
      int targetForwardCount,
      ReplicaInfo replicaInfo,
      GroupMap groupMap,
      GroupProxyRetriever proxyRetriever) {
    this.state = new ReplicaState(targetForwardCount);
    this.replicaInfo = replicaInfo;
    this.groupMap = groupMap;
    this.proxyRetriever = proxyRetriever;

    this.logger =
        new Logger()
            .with("groupID", this.replicaInfo.groupID())
            .with("serverID", this.replicaInfo.serverID());
  }

  @Override
  public byte[][] appExecuteBatch(byte[][] cmds, MessageContext[] ctxs) {
    return Arrays.stream(cmds)
        .map(
            cmd -> {
              Request request;
              try {
                request = Serializer.fromBytes(cmd, Request.class);
              } catch (SerializingException e) {
                logger.with("exception", e.toString()).error("Failed to deserialize request");
                var response =
                    new Response("INVALID_PAYLOAD", new int[] {this.replicaInfo.groupID()});
                var reply = new ReplicaReply.Raw(Serializer.toBytes(response));

                return Serializer.toBytes(reply);
              }

              ReplicaReply reply;
              try {
                reply = this.handleRequest(request);
              } catch (Exception e) {
                logger.with("exception", e.toString()).error("Failed to handle request");
                var response =
                    new Response("INTERNAL_ERROR", new int[] {this.replicaInfo.groupID()});
                var errorReply = new ReplicaReply.Raw(Serializer.toBytes(response));

                return Serializer.toBytes(errorReply);
              }
              return Serializer.toBytes(reply);
            })
        .toArray(byte[][]::new);
  }

  @Override
  public byte[] appExecuteUnordered(byte[] cmd, MessageContext ctx) {
    var response = new Response("UNSUPPORTED_OPERATION", new int[] {this.replicaInfo.groupID()});
    var rawResponse = Serializer.toBytes(response);
    return Serializer.toBytes(new ReplicaReply.Raw(rawResponse));
  }

  @Override
  public byte[] getSnapshot() {

    return Serializer.toBytes(this.state);
  }

  @Override
  public void installSnapshot(byte[] data) {
    try {
      this.state = Serializer.fromBytes(data, ReplicaState.class);
    } catch (SerializingException e) {
      logger.error("Failed to deserialize snapshot", e);
      throw new RuntimeException(e);
    }
  }

  private ReplicaReply handleRequest(Request request) throws SerializingException {
    var logger = this.logger.with("requestID", request.id());

    if (!request.isForwarded()) {
      logger.info("Message is not forwarded.");
      var response = this.handleNonForwardedRequest(request);
      return new ReplicaReply.Raw(Serializer.toBytes(response));
    }

    logger.info("Message is forwarded.");
    var cacheResult = this.state.getReply(request.id());
    if (cacheResult.isPresent()) {
      logger.info("Reply is cached.");
      var result = cacheResult.get();
      return new ReplicaReply.Raw(Serializer.toBytes(result));
    }
    logger.info("Reply is not cached.");

    var isReadyToHandle = this.state.increasePendingRequest(request.id());
    if (!isReadyToHandle) {
      return new ReplicaReply.Pending(request.id());
    }

    logger.info("Handling forwarded message.");
    var response = this.handleNonForwardedRequest(request);
    this.state.cacheReply(request, response);
    return new ReplicaReply.Completed(request.id(), Serializer.toBytes(response));
  }

  private Response handleNonForwardedRequest(Request request) throws SerializingException {
    var targetGroupsIDs =
        Arrays.stream(request.targetGroups())
            .boxed()
            .collect(Collectors.toCollection(ArrayList::new));
    var amTargeted = targetGroupsIDs.remove((Integer) this.replicaInfo.groupID());

    if (amTargeted) {
      this.state.addHandledRequest(request);
    }

    var nextSteps = groupMap.pathsForTargets(this.replicaInfo.groupID(), targetGroupsIDs);
    if (nextSteps.isEmpty()) {
      return new Response("NO_PATH", new int[] {this.replicaInfo.groupID()});
    }

    var passedGroups = new TreeSet<Integer>();
    for (var entry : nextSteps.get().entrySet()) {
      var nextGroup = entry.getKey();
      var targetGroups = entry.getValue().stream().mapToInt(Integer::intValue).toArray();
      var nextRequest = new Request(request.id(), targetGroups, true, request.content());

      var proxy = this.proxyRetriever.forGroup(nextGroup);
      var responseBytes = proxy.invokeOrdered(Serializer.toBytes(nextRequest));
      var response = Serializer.fromBytes(responseBytes, Response.class);

      for (var groupID : response.groupIDs()) {
        passedGroups.add(groupID);
      }
    }

    return new Response("OK", passedGroups.stream().mapToInt(Integer::intValue).toArray());
  }
}
