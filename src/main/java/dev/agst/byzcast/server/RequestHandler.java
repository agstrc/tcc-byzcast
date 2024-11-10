package dev.agst.byzcast.server;

import dev.agst.byzcast.Logger;
import dev.agst.byzcast.Logger.Attr;
import dev.agst.byzcast.Serializer;
import dev.agst.byzcast.group.GroupProxies;
import dev.agst.byzcast.message.Request;
import dev.agst.byzcast.message.Request.ClientRequest;
import dev.agst.byzcast.message.Request.ReplicaRequest;
import dev.agst.byzcast.message.Response.AggregatedResponse;
import dev.agst.byzcast.message.Response.BatchResponse;
import dev.agst.byzcast.message.Response.GroupResponse;
import dev.agst.byzcast.topology.Topology;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RequestHandler {
  private final Logger logger;

  private final int groupID;

  private final Topology topology;

  private final GroupProxies proxies;

  private final ServerReplier replier;

  private final ExecutorService executor = Executors.newCachedThreadPool();

  private final TreeMap<UUID, ArrayList<ReplicaRequest>> pendingRRs = new TreeMap<>();

  public RequestHandler(
      Logger logger, int groupID, Topology topology, GroupProxies proxies, ServerReplier replier) {
    this.logger = logger;
    this.groupID = groupID;
    this.topology = topology;
    this.proxies = proxies;
    this.replier = replier;
  }

  public List<ServerReply> handle(List<Request> requests, ServerState state) {
    var readyRequests = computeReadyRequests(requests, state);
    var responses = handleRequestsLocally(readyRequests, state);

    var batchedRequests = batchRequests(readyRequests);
    forwardRequests(batchedRequests, responses);

    var requestsFromReplicas =
        requests.stream()
            .filter(r -> r instanceof ReplicaRequest)
            .flatMap(
                r -> {
                  var rr = (ReplicaRequest) r;
                  return rr.requests().stream();
                })
            .toList();

    for (var request : requestsFromReplicas) {
      var response = responses.get(request);
      if (response == null) continue;

      state.setCachedResponse(request.id(), response);
    }

    var replies = new ArrayList<ServerReply>();
    for (var request : requests) {
      if (request instanceof ClientRequest) {
        var response = responses.get((ClientRequest) request);
        replies.add(new ServerReply.Done(Serializer.toBytes(response)));
        continue;
      }

      replies.add(computeReply((ReplicaRequest) request, state));
    }

    for (var handledRequest : responses.keySet()) {
      var pendingList = pendingRRs.remove(handledRequest.id());
      if (pendingList == null) continue;

      for (var rr : pendingList) {
        var reply = computeReply(rr, state);
        if (reply instanceof ServerReply.Done) {
          var doneReply = (ServerReply.Done) reply;
          replier.sendReply(rr.id(), doneReply.data());
        }
      }
    }

    return replies;
  }

  private ServerReply computeReply(ReplicaRequest rr, ServerState state) {
    var responses = new ArrayList<AggregatedResponse>();

    for (ClientRequest cr : rr.requests()) {
      var response = state.getCachedResponse(cr.id());
      if (response.isEmpty()) {
        var pendingList = pendingRRs.computeIfAbsent(cr.id(), k -> new ArrayList<>());

        // We only add the request to the pending list if it is not already there.
        // A huge number of pending requests for a given key is not expected, so the linear search
        // should be fine.
        var containsReplicaRequest = pendingList.stream().anyMatch(req -> req.id().equals(rr.id()));
        if (!containsReplicaRequest) pendingList.add(rr);

        return new ServerReply.Pending(rr.id());
      }

      responses.add(response.get());
    }

    var batchResponse = new BatchResponse(responses);
    var reply = new ServerReply.Done(Serializer.toBytes(batchResponse));

    rr.requests().forEach(cr -> state.increaseReturnCount(cr.id()));

    return reply;
  }

  private ArrayList<ClientRequest> computeReadyRequests(List<Request> requests, ServerState state) {
    // Implementation note: in general, TreeSets and TreeMaps are used in order to keep behaviour
    // consistent between replicas. Usage of Hash* data structures may lead to different ordering
    // of requests.

    // readyIDs makes sure we do not repeat requests that are already ready to be handled.
    // I am actually not sure if this is necessary, I have hypothesized scenarios in which a
    // request may appear more than once in the list of requests, but I am not sure if this is
    // possible.
    var readyIDs = new TreeSet<UUID>();
    var readyRequests = new ArrayList<ClientRequest>();

    for (var request : requests) {
      logger.info("Request received", new Attr("request", request));

      if (request instanceof ClientRequest) {
        logger.info("Request ready to be handled", new Attr("request", request));

        // ClientRequests aren't repeated, therefore they can safely be added directly and without
        // checking the set.
        readyRequests.add((ClientRequest) request);
        continue;
      }

      var replicaRequest = (Request.ReplicaRequest) request;
      for (var clientRequest : replicaRequest.requests()) {
        if (state.getCachedResponse(clientRequest.id()).isPresent()) {
          logger.info("Request already handled", new Attr("request", clientRequest));
          continue;
        }

        var shouldHandle = state.enqueue(clientRequest.id());
        if (shouldHandle && !readyIDs.contains(clientRequest.id())) {
          logger.info("Request ready to be handled", new Attr("request", clientRequest));
          readyRequests.add(clientRequest);
          readyIDs.add(clientRequest.id());
        }
      }
    }

    return readyRequests;
  }

  private TreeMap<ClientRequest, AggregatedResponse> handleRequestsLocally(
      ArrayList<ClientRequest> requests, ServerState state) {
    var comparator = Comparator.comparing(ClientRequest::id);
    var responses = new TreeMap<ClientRequest, AggregatedResponse>(comparator);

    for (var request : requests) {
      if (request.targetGroups().contains(this.groupID)) {
        logger.info("Request handled locally", new Attr("request", request));
        state.markAsHandled(request.id());

        request.targetGroups().remove((Integer) this.groupID);

        var response = new AggregatedResponse("HANDLED", new ArrayList<>());
        responses.put(request, response);
      } else {
        var response = new AggregatedResponse("FORWARDED", new ArrayList<>());
        responses.put(request, response);
      }
    }

    return responses;
  }

  private static record GroupReplicaRequest(int groupID, ReplicaRequest request) {}

  private ArrayList<GroupReplicaRequest> batchRequests(ArrayList<ClientRequest> requests) {
    var groupedRequests = new TreeMap<Integer, ArrayList<ClientRequest>>();

    for (var request : requests) {
      if (request.targetGroups().isEmpty()) continue;

      var optDownstreamPaths = topology.findPaths(this.groupID, request.targetGroups());
      if (optDownstreamPaths.isEmpty()) {
        logger.error("No downstream paths found", new Attr("request", request));
        continue;
      }

      var downstreamPaths = optDownstreamPaths.get();
      for (var entry : downstreamPaths.entrySet()) {
        var groupID = entry.getKey();
        var path = new ArrayList<>(entry.getValue());

        var updatedRequest = new ClientRequest(request.id(), path, request.content());
        var requestList = groupedRequests.computeIfAbsent(groupID, k -> new ArrayList<>());
        requestList.add(updatedRequest);
      }
    }

    var replicaRequests = new ArrayList<GroupReplicaRequest>();
    for (var entry : groupedRequests.entrySet()) {
      var groupID = entry.getKey();
      var requestList = entry.getValue();
      var replicaRequest = new ReplicaRequest(requestList);
      replicaRequests.add(new GroupReplicaRequest(groupID, replicaRequest));
    }

    return replicaRequests;
  }

  private void forwardRequests(
      ArrayList<GroupReplicaRequest> requests,
      TreeMap<ClientRequest, AggregatedResponse> responses) {

    var downstreamResponses =
        requests.stream()
            .map(
                request -> {
                  var groupID = request.groupID();
                  var replicaRequest = request.request();

                  var proxy = proxies.forGroup(groupID);
                  var dispatcher = new RequestDispatcher(proxy);
                  logger.info("Forwarding request", new Attr("request", replicaRequest));

                  return executor.submit(
                      () -> {
                        try {
                          return dispatcher.dispatch(replicaRequest);
                        } catch (Exception e) {
                          var message =
                              String.format(
                                  "Replica request failed. ID=%s. Request=%s",
                                  replicaRequest.id().toString(), replicaRequest.toString());
                          var contextualException = new Exception(message);

                          contextualException.addSuppressed(e);
                          throw contextualException;
                        }
                      });
                })
            .toList() // collect the futures before getting them to avoid blocking
            .stream()
            .map(
                future -> {
                  try {
                    return future.get();
                  } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                  }
                })
            .toList();

    for (int i = 0; i < requests.size(); i++) {
      var batchResponse = downstreamResponses.get(i);
      var batchRequest = requests.get(i).request();
      var requestGroup = requests.get(i).groupID();

      if (batchResponse.responses().size() != batchRequest.requests().size()) {
        // some error handling here would be nice
        logger.error(
            "Mismatched response size",
            new Attr("request", batchRequest),
            new Attr("response", batchResponse));
        throw new RuntimeException("Mismatched response size");
      }

      for (int j = 0; j < batchResponse.responses().size(); j++) {
        var individualResponse = batchResponse.responses().get(j);
        var individualRequest = batchRequest.requests().get(j);

        responses
            .get(individualRequest)
            .downstreamResponses()
            .add(new GroupResponse(requestGroup, individualResponse));
      }
    }
  }
}
