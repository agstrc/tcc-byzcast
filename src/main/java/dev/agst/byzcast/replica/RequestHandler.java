package dev.agst.byzcast.replica;

import bftsmart.tom.ServiceProxy;
import dev.agst.byzcast.Logger;
import dev.agst.byzcast.Serializer;
import dev.agst.byzcast.group.GroupProxies;
import dev.agst.byzcast.message.Request;
import dev.agst.byzcast.message.Response;
import dev.agst.byzcast.message.Response.GroupResponse;
import dev.agst.byzcast.topology.Topology;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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

  private final GroupProxies proxies;
  private final Topology topology;

  /** The executor responsible for async requests sent upstream */
  private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

  public RequestHandler(Logger logger, ReplicaInfo info, GroupProxies proxies, Topology topology) {
    this.logger = logger;
    this.info = info;
    this.proxies = proxies;
    this.topology = topology;
  }

  /**
   * Handles an incoming request by determining its source and processing it accordingly. If the
   * request originates from a client, it is processed immediately. If the request comes from a
   * replica, it checks for a cached response before deciding to process or enqueue the request
   * based on its readiness. This method logs the request handling process and returns a {@link
   * ReplicaReply} indicating the outcome.
   *
   * @param request The {@link Request} object representing the incoming request.
   * @param state The current {@link ReplicaState} of the node, used for managing request states and
   *     caching.
   * @return A {@link ReplicaReply} object representing the outcome of the request handling. This
   *     could be a raw response, a pending status, or a completed response.
   */
  public ReplicaReply handle(Request request, ReplicaState state) {
    var logger = this.logger.with("RID", request.id());

    if (request.source() == Request.Source.CLIENT) {
      logger.info("Request is client request");
      var response = this.handleReadyRequest(request, state);
      return new ReplicaReply.Raw(Serializer.toBytes(response));
    }

    logger = logger.with("source", "REPLICA");
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

  /**
   * Processes a request that is ready for handling. This method determines if the current node is
   * targeted by the request and marks it as handled if so. If there are further target groups, it
   * forwards the request to them. This method is responsible for generating a {@link Response}
   * based on the request's target groups and the current node's ability to handle or forward the
   * request.
   *
   * @param request The {@link Request} object that is ready to be processed.
   * @param state The current {@link ReplicaState} of the node, used for marking requests as
   *     handled.
   * @return A {@link Response} object representing the outcome of processing the request. This
   *     could be an immediate response if the node is a target, or a composite response from
   *     forwarding the request to other target groups.
   */
  private Response handleReadyRequest(Request request, ReplicaState state) {
    var targetGroups =
        Arrays.stream(request.targetGroups())
            .boxed()
            .collect(Collectors.toCollection(ArrayList::new));
    var amTargeted = targetGroups.remove((Integer) this.info.groupID());

    if (amTargeted) {
      state.markAsHandled(request);
    }

    var responseContent = amTargeted ? "HANDLED" : "FORWARDED";
    if (targetGroups.isEmpty()) {
      return new Response(responseContent, new ArrayList<>());
    }

    var optNextGroups = this.topology.findPaths(this.info.groupID(), targetGroups);
    if (optNextGroups.isEmpty()) {
      return new Response("NO_PATH", new ArrayList<>());
    }

    var nextGroups = optNextGroups.get().entrySet();
    var groupResponses = forwardToGroups(request, nextGroups);
    return new Response(responseContent, groupResponses);
  }

  /**
   * Forwards the given request to the specified groups concurrently using virtual threads. This
   * method prepares a new request for each target group and submits it for processing via an
   * executor. The method collects responses from each targeted group, handling any exceptions that
   * occur during forwarding. A composite response is constructed from the individual group
   * responses and returned. If a forwarding operation fails for any group, a default response
   * indicating failure is included for that group.
   *
   * @param request The original request to be forwarded.
   * @param nextGroups A set of entries where each entry contains a group ID and a list of target
   *     group IDs for forwarding. This set determines the next groups to which the request should
   *     be forwarded and the subsequent target groups for each of those forwards.
   * @return An ArrayList of GroupResponse objects that aggregates the responses from all targeted
   *     groups.
   */
  private ArrayList<GroupResponse> forwardToGroups(
      Request request, Set<Entry<Integer, List<Integer>>> nextGroups) {
    // I don't really like all this indentation, but my formatter wills it so it is what it is
    var responseStream =
        nextGroups.stream()
            .map(
                nextGroup -> {
                  return forwardToGroup(request, nextGroup.getKey(), nextGroup.getValue());
                })
            .map(
                future -> {
                  try {
                    return future.get();
                  } catch (Exception e) {
                    // the only "acceptable" exception here would be interrupted exception, as the
                    // inner method forwardToGroup should handle all other exceptions
                    throw new RuntimeException(e);
                  }
                });

    return responseStream.collect(Collectors.toCollection(ArrayList::new));
  }

  /**
   * Asynchronous wrap and boilerplate for {@link #forwardToGroup(Request, int, ServiceProxy)} to
   * allow for concurrent request forwarding.
   *
   * @param localReq The {@link Request} object to be sent upstream.
   * @param targetGroupID The target group ID to which the request should be forwarded.
   * @param targetGroups A list of desired target group IDs expected to be reached from the target
   * @return A {@link Future<GroupResponse>} object representing the asynchronous operation of
   *     forwarding the request to the target group.
   */
  private Future<GroupResponse> forwardToGroup(
      Request localReq, int targetGroupID, List<Integer> targetGroups) {
    var groupsArray = targetGroups.stream().mapToInt(Integer::intValue).toArray();
    var request =
        new Request(localReq.id(), groupsArray, localReq.content(), Request.Source.REPLICA);
    var proxy = this.proxies.forGroup(targetGroupID);
    return executor.submit(() -> forwardToGroup(request, targetGroupID, proxy));
  }

  /**
   * Sends a request to a specified upstream group via the provided service proxy. This method
   * attempts to send the request and waits for a response. If the operation is successful, a
   * GroupResponse containing the group ID and the response is returned. In case of any exception,
   * an error GroupResponse is generated to ensure that the system can gracefully handle failures.
   *
   * @param request The request to be sent upstream.
   * @param groupID The ID of the target group to which the request is sent.
   * @param proxy The ServiceProxy used to send the request to the target group.
   * @return A GroupResponse object representing the outcome of the request. This includes the group
   *     ID and either the received response or an error message.
   */
  private GroupResponse forwardToGroup(Request request, int groupID, ServiceProxy proxy) {
    try {
      var responseBytes = proxy.invokeOrdered(Serializer.toBytes(request));
      var response = Serializer.fromBytes(responseBytes, Response.class);
      return new GroupResponse(groupID, response);
    } catch (Exception e) {
      this.logger.error("Failed to handle request", e);
      var responseContent = String.format("REQUEST_TO_GROUP_%d_FAILED", groupID);
      return new GroupResponse(groupID, new Response(responseContent, new ArrayList<>()));
    }
  }
}
