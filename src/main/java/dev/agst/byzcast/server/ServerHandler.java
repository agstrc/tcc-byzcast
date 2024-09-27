package dev.agst.byzcast.server;

import dev.agst.byzcast.Global;
import dev.agst.byzcast.Logger;
import dev.agst.byzcast.Logger.Attr;
import dev.agst.byzcast.Serializer;
import dev.agst.byzcast.group.GroupProxies;
import dev.agst.byzcast.replica.ReplicaInfo;
import dev.agst.byzcast.replica.ReplicaReply;
import dev.agst.byzcast.topology.Topology;
import dev.agst.byzcast.v2.message.Request;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

public class ServerHandler {
  private final Logger logger;

  private final ReplicaInfo info;

  private final GroupProxies proxies;

  private final Topology topology;

  private final ArrayList<Request.Batch> pendingBatches = new ArrayList<>();

  // private final Map<UUID, List<Request.Batch>> batchRequests = new TreeMap<>();

  public ServerHandler(Logger logger, ReplicaInfo info, GroupProxies proxies, Topology topology) {
    this.logger = logger;
    this.info = info;
    this.proxies = proxies;
    this.topology = topology;
  }

  public List<ReplicaReply> handle(List<Request> requests, ServerState state) {
    var requestsToHandle = new TreeSet<Request.Single>();

    for (var req : requests) {
      if (req instanceof Request.Single) {
        var singleReq = (Request.Single) req;
        var logger = this.logger.with(new Attr("RID", singleReq.id()));

        logger.info("Request received");
        if (state.getCachedResponse(singleReq.id()).isPresent()) {
          logger.info("Request is cached");
          continue;
        }

        logger.info("Request will be handled");
        requestsToHandle.add(singleReq);
        continue;
      }

      if (req instanceof Request.Batch) {
        var logger = this.logger.with(new Attr("Batch", true));

        var batch = (Request.Batch) req;
        for (var reqInBatch : batch.requests()) {
          logger.info("Request received", new Attr("RID", reqInBatch.id()));

          if (state.getCachedResponse(reqInBatch.id()).isPresent()) {
            logger.info("Request is cached", new Attr("RID", reqInBatch.id()));
            continue;
          }

          var shouldHandle = state.enqueue(reqInBatch.id());
          if (shouldHandle) {
            logger.info("Request will be handled", new Attr("RID", reqInBatch.id()));
            requestsToHandle.add((Request.Single) reqInBatch);
          }
        }
      }
    }

    handleReadyRequests(requestsToHandle, state);

    // requests.addAll(pendingBatches);
    // pendingBatches.clear();

    // var originalRequests =
    //     requests.stream()
    //         .map(
    //             (r) -> {
    //               return switch (r) {
    //                 case Request.Single single -> replyFromSingle(single, state);
    //                 case Request.Batch batch -> replyFromBatch(batch, state);
    //               };
    //             })
    //         .toList();

    // System.out.println("BEGIN EXTRA");
    // var pendingAf =
    //     pendingBatches.stream()
    //         .map(
    //             (r) -> {
    //               return switch (r) {
    //                 case Request.Batch batch -> replyFromBatch(batch, state);
    //               };
    //             })
    //         .toList();
    // System.out.println("END EXTRA");

    // originalRequests.addAll(pendingAf);
    // var fuckIt = new ArrayList<>(originalRequests);
    // fuckIt.addAll(pendingAf);
    // requests.addAll(pendingBatches);
    // pendingBatches.clear();

    // deal with all pending requests
    var localPending = new ArrayList<>(pendingBatches);
    pendingBatches.clear();

    for (var pending : localPending) {
      var reply = replyFromBatch(pending, state);
      if (reply instanceof ReplicaReply.Completed) {
        var completedReply = (ReplicaReply.Completed) reply;
        Global.replier.processCompletedReply(completedReply);
      }
    }

    return requests.stream()
        .map(
            (r) -> {
              return switch (r) {
                case Request.Single single -> replyFromSingle(single, state);
                case Request.Batch batch -> replyFromBatch(batch, state);
              };
            })
        .toList();
  }

  private void handleReadyRequests(TreeSet<Request.Single> requestsToHandle, ServerState state) {
    var currentGroup = info.groupID();

    var requestResponses = new TreeMap<Request.Single, String>();

    var groupToRequests = new TreeMap<Integer, ArrayList<Request.Single>>();

    for (var request : requestsToHandle) {
      if (request.targetGroups().contains(currentGroup)) {
        requestResponses.put(request, "HANDLED_BY_" + currentGroup);
        request.targetGroups().remove((Integer) currentGroup);

        logger.info("Request locally handled", new Attr("RID", request.id()));
        state.markAsHandled(request.id());
      } else {
        requestResponses.put(request, "FORWARDED_BY_" + currentGroup);
      }

      var targetGroups = request.targetGroups();
      if (targetGroups.isEmpty()) continue;

      // TODO: error handling. What should we do if there is no path?
      var nextGroupPaths = topology.findPaths(currentGroup, targetGroups).get();
      for (var entry : nextGroupPaths.entrySet()) {
        var nextGroup = entry.getKey();
        var pathFromNextGroup = entry.getValue();

        var requestList = groupToRequests.computeIfAbsent(nextGroup, (k) -> new ArrayList<>());
        var modifiedRequest =
            new Request.Single(request.id(), new ArrayList<>(pathFromNextGroup), request.content());
        requestList.add(modifiedRequest);
      }
    }

    var syncRequestResponses = Collections.synchronizedMap(requestResponses);
    var threads = new ArrayList<Thread>();
    for (var entry : groupToRequests.entrySet()) {
      var group = entry.getKey();
      var requests = entry.getValue();

      var batchRequest = new Request.Batch(requests);
      var proxy = proxies.forGroup(group);
      var thread =
          Thread.ofVirtual()
              .start(
                  () -> {
                    var request = Serializer.toBytes(batchRequest);
                    var reply = proxy.invokeOrdered(request);
                    if (reply == null) {
                      var requestIDs = requests.stream().map(r -> r.id().toString()).toList();
                      logger.info(
                          "BATCH timed out",
                          new Attr("RequestIDs", requestIDs),
                          new Attr("Group", group));
                    }

                    for (var req : requests) {
                      var localResponse = syncRequestResponses.get(req);
                      var response = new String(reply) + localResponse;
                      syncRequestResponses.put(req, response);
                    }
                  });
      threads.add(thread);
    }

    threads.forEach(
        t -> {
          try {
            t.join();
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
        });

    for (var entry : requestResponses.entrySet()) {
      var request = entry.getKey();
      var response = entry.getValue();
      logger.info("Cached response", new Attr("RID", request.id()));
      state.cacheResponse(request.id(), response.getBytes());
    }
  }

  private ReplicaReply replyFromSingle(Request.Single single, ServerState state) {
    logger.info("Request queried", new Attr("RID", single.id()));
    var response = state.getCachedResponse(single.id());
    if (response.isEmpty()) {
      logger.error("Request not found in cache", new Attr("RID", single.id()));
      return new ReplicaReply.Pending(single.id());
    }

    logger.info("Request found in cache", new Attr("RID", single.id()));
    return new ReplicaReply.Completed(single.id(), "OK".getBytes());
  }

  private ReplicaReply replyFromBatch(Request.Batch batch, ServerState state) {
    for (var req : batch.requests()) {
      logger.info("Request queried BATCH", new Attr("RID", req.id()));
      var response = state.getCachedResponse(req.id());
      if (response.isEmpty()) {
        // logger.info("Request not found in cache", new Attr("RID", req.id()));

        var requestIDs = batch.requests().stream().map(r -> r.id().toString()).toList();
        logger.info(
            "Request not found in cache",
            new Attr("RequestIDs", requestIDs),
            new Attr("Faulty", req.id()));
        var uuid = batchUUID(batch);
        pendingBatches.add(batch);

        return new ReplicaReply.Pending(uuid);
      }
    }

    var requestIDs = batch.requests().stream().map(r -> r.id().toString()).toList();
    logger.info(
        "All requests found in cache",
        new Attr("RequestIDs", requestIDs),
        new Attr("RID", batchUUID(batch)));
    return new ReplicaReply.Completed(batchUUID(batch), "OK".getBytes());
  }

  /**
   * Generates a UUID based on the concatenated UUIDs of all requests in the given batch.
   *
   * <p>This method iterates over all requests in the provided {@link Request.Batch}, extracts their
   * UUIDs, and concatenates their byte representations into a single byte array. It then generates
   * a new UUID from this byte array using {@link UUID#nameUUIDFromBytes(byte[])}.
   *
   * @param batch the batch of requests from which to generate the UUID
   * @return a UUID based on the concatenated UUIDs of all requests in the batch
   */
  private static UUID batchUUID(Request.Batch batch) {
    try (var stream = new ByteArrayOutputStream()) {
      for (var request : batch.requests()) {
        var uuid = request.id();
        var buffer = ByteBuffer.allocate(16);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());

        stream.write(buffer.array());
      }
      return UUID.nameUUIDFromBytes(stream.toByteArray());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
