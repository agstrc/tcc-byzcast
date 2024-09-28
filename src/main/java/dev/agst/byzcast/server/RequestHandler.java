package dev.agst.byzcast.server;

import dev.agst.byzcast.Logger;
import dev.agst.byzcast.Logger.Attr;
import dev.agst.byzcast.Serializer;
import dev.agst.byzcast.SerializingException;
import dev.agst.byzcast.group.GroupProxies;
import dev.agst.byzcast.message.Request;
import dev.agst.byzcast.message.Request.ClientRequest;
import dev.agst.byzcast.message.Request.ReplicaRequest;
import dev.agst.byzcast.message.Response.AggregatedResponse;
import dev.agst.byzcast.message.Response.BatchResponse;
import dev.agst.byzcast.message.Response.GroupResponse;
import dev.agst.byzcast.topology.Topology;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class RequestHandler {
  private final Logger logger;

  private final int groupID;

  private final GroupProxies proxies;

  private final Topology topology;

  private final ServerReplier replier;

  /**
   * Maps a request's UUID to the list of replica requests that are waiting on its completion. This
   * map is used to track the status of requests and ensure that all replica requests are properly
   * handled once their requests are completed.
   */
  private final TreeMap<UUID, ArrayList<Request.ReplicaRequest>> pending = new TreeMap<>();

  private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

  public RequestHandler(
      Logger logger, int groupID, GroupProxies proxies, Topology topology, ServerReplier replier) {
    this.logger = logger;
    this.groupID = groupID;
    this.proxies = proxies;
    this.topology = topology;
    this.replier = replier;
  }

  public List<ServerReply> handle(List<Request> requests, ServerState state) {
    System.out.println("emall " + requests);

    var ready = filterReadyRequests(requests, state);
    handleReadyRequests(ready, state);

    // A note on implementation.
    // Ideally, it'd be better to have an improved tracking on requests and responses, and keeping
    // a request's ID along to its response only for as long as needed. The current implementation
    // uses an LRU cache which can be wasteful and also lead to errors in case of higher batches
    // or insufficient cache capacity. Since this logic of caching responses and enqueueing requests
    // is made in order to only reply to a replica request once N-F requests have been received, an
    // ideal implementation would remove the request/response pair from cache either once N requests
    // are received, or after a considerable timeout.

    var replies =
        requests.stream().map(request -> replyFor(request, state)).collect(Collectors.toList());

    ready.forEach(
        handledRequest -> {
          var id = handledRequest.id();
          var pendingRequests = pending.get(id);

          if (pendingRequests == null) return;
          pending.remove(id);

          pendingRequests.forEach(
              replicaRequest -> {
                var reply = replyFor(replicaRequest, state);
                if (reply instanceof ServerReply.Pending) return;

                logger.info("Sending pending", new Attr("RR", replicaRequest));
                var doneReply = (ServerReply.Done) reply;
                replier.sendReply(replicaRequest.id(), doneReply.content());
              });
        });

    return replies;
  }

  private TreeSet<ClientRequest> filterReadyRequests(List<Request> requests, ServerState state) {
    var ready = new TreeSet<ClientRequest>();

    for (var request : requests) {
      if (request instanceof ClientRequest) {
        var clientRequest = (ClientRequest) request;
        var idAttr = new Attr("RID", clientRequest.id());

        logger.info("Got request", idAttr, new Attr("fromBatch", false));
        logger.info("Will handle request", idAttr);

        ready.add((ClientRequest) request);
        continue;
      }

      var replicaRequest = (Request.ReplicaRequest) request;
      for (var clientRequest : replicaRequest.requests()) {
        var idAttr = new Attr("RID", clientRequest.id());

        logger.info("Got request", idAttr, new Attr("fromBatch", true));

        if (state.getCachedResponse(clientRequest.id()).isPresent()) {
          logger.info("Request already handled", idAttr);
          continue;
        }

        var shouldHandle = state.enqueue(clientRequest.id());

        if (shouldHandle) {
          logger.info("Will handle request", idAttr);
          ready.add(clientRequest);
        }
      }
    }

    return ready;
  }

  private void handleReadyRequests(TreeSet<ClientRequest> requests, ServerState state) {
    var localResponses = locallyHandleRequests(requests, state);
    var groupedRequests = mapGroupsToRequests(requests, localResponses);
    forwardRequests(groupedRequests, localResponses, state);
  }

  /**
   * Locally handles all requests that are marked for the current group. This method marks the
   * requests as handled and modifies the request object's inner ArrayList. It then returns a map
   * containing the UUIDs of the requests and their corresponding AggregatedResponses, indicating
   * whether each request was handled or forwarded.
   *
   * @param requests the set of client requests to be handled
   * @param state the current server state
   * @return a TreeMap containing the UUIDs of the requests and their corresponding
   *     AggregatedResponses
   */
  private TreeMap<UUID, AggregatedResponse> locallyHandleRequests(
      TreeSet<ClientRequest> requests, ServerState state) {
    var localResponses = new TreeMap<UUID, AggregatedResponse>();

    for (var request : requests) {
      if (request.targetGroups().contains(this.groupID)) {
        state.markAsHandled(request.id());
        request.targetGroups().remove((Integer) this.groupID);

        logger.info("Request handled by node", new Attr("RID", request.id()));

        var response = new AggregatedResponse("HANDLED", new ArrayList<>());
        localResponses.put(request.id(), response);
      } else {
        var response = new AggregatedResponse("FORWARDED", new ArrayList<>());
        localResponses.put(request.id(), response);
      }
    }

    return localResponses;
  }

  private TreeMap<Integer, ArrayList<ClientRequest>> mapGroupsToRequests(
      TreeSet<ClientRequest> requests, TreeMap<UUID, AggregatedResponse> localResponses) {
    var groupToRequests = new TreeMap<Integer, ArrayList<ClientRequest>>();

    for (var request : requests) {
      if (request.targetGroups().isEmpty()) continue;

      var optDownstreamPaths = topology.findPaths(this.groupID, request.targetGroups());
      if (optDownstreamPaths.isEmpty()) {
        logger.error(
            "Failed to find path for requests",
            new Attr("RID", request.id()),
            new Attr("targetGroups", request.targetGroups()),
            new Attr("groupID", this.groupID));

        var oldResponse = localResponses.get(request.id());
        var newContent = "ERROR_NO_PATH :: " + oldResponse.content();
        var updatedResponse = new AggregatedResponse(newContent, oldResponse.downstreamResponses());
        localResponses.put(request.id(), updatedResponse);
        continue;
      }

      var downstreamPaths = optDownstreamPaths.get();
      for (var entry : downstreamPaths.entrySet()) {
        var nextGroup = entry.getKey();
        var pathFromNextGroup = new ArrayList<>(entry.getValue());

        var requestList = groupToRequests.computeIfAbsent(nextGroup, k -> new ArrayList<>());
        var updatedRequest = new ClientRequest(request.id(), pathFromNextGroup, request.content());
        requestList.add(updatedRequest);
      }
    }

    return groupToRequests;
  }

  private void forwardRequests(
      TreeMap<Integer, ArrayList<ClientRequest>> groupsToRequests,
      TreeMap<UUID, AggregatedResponse> localResponses,
      ServerState state) {

    var futures =
        groupsToRequests.entrySet().stream()
            .map(
                entry -> {
                  var groupID = entry.getKey();
                  var requests = entry.getValue();

                  var proxy = proxies.forGroup(groupID);
                  var replicaReq = new ReplicaRequest(requests);

                  return executor.submit(
                      () -> {
                        var rawResponse = proxy.invokeOrdered(Serializer.toBytes(replicaReq));
                        if (rawResponse == null) {
                          logger.error("Request timed out", new Attr("req", replicaReq));
                          // TODO: handle response timeout
                        }

                        BatchResponse response;
                        try {
                          response = Serializer.fromBytes(rawResponse, BatchResponse.class);
                        } catch (SerializingException e) {
                          throw new RuntimeException(e);
                        }

                        for (int i = 0; i < requests.size(); i++) {
                          var request = requests.get(i);
                          var resp = response.responses().get(i);

                          var requestResponse = localResponses.get(request.id());
                          var syncList =
                              Collections.synchronizedList(requestResponse.downstreamResponses());

                          syncList.add(new GroupResponse(groupID, resp));
                          syncList.sort(GroupResponse::compareTo);
                        }
                      });
                })
            .toList();

    for (var future : futures) {
      try {
        future.get();
      } catch (InterruptedException | ExecutionException e) {
        throw new RuntimeException(e);
      }
    }

    for (var entry : localResponses.entrySet()) {
      var id = entry.getKey();
      var response = entry.getValue();

      state.cacheResponse(id, response);
    }
  }

  private ServerReply replyFor(Request request, ServerState state) {
    if (request instanceof ClientRequest) {
      var clientRequest = (ClientRequest) request;
      var response = state.getCachedResponse(clientRequest.id()).get();

      System.out.println("Reply: " + clientRequest.id());
      System.out.println(response);

      return new ServerReply.Done(Serializer.toBytes(response));
    }

    var replicaRequest = (Request.ReplicaRequest) request;
    var responses = new ArrayList<AggregatedResponse>();

    for (var req : replicaRequest.requests()) {
      var optResponse = state.getCachedResponse(req.id());
      if (optResponse.isEmpty()) {
        logger.info("Response not cached", new Attr("RID", req.id()));
        var pendingList = pending.computeIfAbsent(req.id(), k -> new ArrayList<>());
        pendingList.add(replicaRequest);

        return new ServerReply.Pending(replicaRequest.id());
      }

      responses.add(optResponse.get());
    }

    logger.info("All responses cached", new Attr("RR", replicaRequest));
    return new ServerReply.Done(Serializer.toBytes(new BatchResponse(responses)));
  }
}
