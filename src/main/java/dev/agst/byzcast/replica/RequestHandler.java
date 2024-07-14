package dev.agst.byzcast.replica;

import dev.agst.byzcast.Logger;
import dev.agst.byzcast.Serializer;
import dev.agst.byzcast.SerializingException;
import dev.agst.byzcast.message.Request;
import dev.agst.byzcast.message.Response;
import dev.agst.byzcast.topology.Topology;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * The {@code RequestHandler} class orchestrates the logical processing of requests within a ByzCast
 * Node. It encapsulates the essential logic for request management, including the assessment of
 * request readiness for processing and the delegation of requests to other nodes as necessary. This
 * class collaborates closely with {@link ReplicaNode} and {@link ReplicaReplier} to ensure
 * efficient and accurate request handling.
 */
public class RequestHandler {
  private final Logger logger;

  private final ReplicaInfo info;
  private final Topology topology;

  public RequestHandler(ReplicaInfo info, Topology topology) {
    this.info = info;
    this.topology = topology;

    this.logger = new Logger().with("groupID", info.groupID()).with("serverID", info.serverID());
  }

  public ReplicaReply handle(Request request, ReplicaState state) throws SerializingException {
    var logger = this.logger.with("requestID", request.id());

    if (!request.isForwarded()) {
      logger.info("Request is not forwarded");
      var response = this.handleReadyRequest(request, state);
      return new ReplicaReply.Raw(Serializer.toBytes(response));
    }

    logger.info("Request is forwarded");
    var optCachedResponse = state.getCachedResponse(request.id());
    if (optCachedResponse.isPresent()) {
      logger.info("Response is cached");
      var response = optCachedResponse.get();
      return new ReplicaReply.Raw(Serializer.toBytes(response));
    }

    logger.info("Response is not cached");
    var isReadyToHandle = state.enqueue(request);
    if (!isReadyToHandle) {
      return new ReplicaReply.Pending(request.id());
    }

    logger.info("Request has reached minimum receive count");
    var response = this.handleReadyRequest(request, state);
    state.cacheResponse(request, response);
    return new ReplicaReply.Completed(request.id(), Serializer.toBytes(response));
  }

  private Response handleReadyRequest(Request request, ReplicaState state)
      throws SerializingException {
    var targetGroups =
        Arrays.stream(request.targetGroups())
            .boxed()
            .collect(Collectors.toCollection(ArrayList::new));
    var amTargeted = targetGroups.remove((Integer) this.info.groupID());

    // TODO: reimplementar response para que tenha uma mapa de grupos e respostas,
    // marcando a resposta de cada grupo a partir daqui

    if (amTargeted) {
      state.markAsHandled(request);
    }

    var optNextSteps = this.topology.pathsForTargets(this.info.groupID(), targetGroups);
    if (optNextSteps.isEmpty()) {
      return new Response("NO_PATH", new int[] {this.info.groupID()});
    }
    var nextSteps = optNextSteps.get();

    var passedGroups = new ArrayList<Integer>();
    passedGroups.add(this.info.groupID());
    for (var next : nextSteps.entrySet()) {
      var nextGroup = next.getKey();
      var nextTargetGroups = next.getValue().stream().mapToInt(Integer::intValue).toArray();
      var nextRequest = new Request(request.id(), nextTargetGroups, true, request.content());

      // TODO: handle invalid upstream response with new response model
      var proxy = this.topology.getServiceProxy(nextGroup);
      var responseBytes = proxy.invokeOrdered(Serializer.toBytes(nextRequest));
      var response = Serializer.fromBytes(responseBytes, Response.class);

      for (var groupID : response.groupIDs()) {
        passedGroups.add(groupID);
      }
    }

    return new Response("OK", passedGroups.stream().mapToInt(Integer::intValue).toArray());
  }
}
