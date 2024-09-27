// package dev.agst.byzcast.v2.replica_;

// import dev.agst.byzcast.Logger;
// import dev.agst.byzcast.Logger.Attr;
// import dev.agst.byzcast.Serializer;
// import dev.agst.byzcast.group.GroupProxies;
// import dev.agst.byzcast.replica.ReplicaInfo;
// import dev.agst.byzcast.replica.ReplicaReply;
// import dev.agst.byzcast.topology.Topology;
// import dev.agst.byzcast.v2.message.Request;
// import java.nio.ByteBuffer;
// import java.security.MessageDigest;
// import java.util.ArrayList;
// import java.util.Arrays;
// import java.util.Collections;
// import java.util.List;
// import java.util.Map;
// import java.util.TreeMap;
// import java.util.TreeSet;
// import java.util.UUID;
// import java.util.concurrent.ExecutorService;
// import java.util.concurrent.Executors;
// import java.util.concurrent.Future;
// import java.util.stream.Collectors;

// public class RequestHandler {
//   private final Logger logger;

//   private final ReplicaInfo info;

//   private final GroupProxies proxies;
//   private final Topology topology;

//   private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
//   private final TreeMap<Request.Single, byte[]> pending = new TreeMap<>();

//   public RequestHandler(Logger logger, ReplicaInfo info, GroupProxies proxies, Topology topology) {
//     this.logger = logger;
//     this.info = info;
//     this.proxies = proxies;
//     this.topology = topology;
//   }

//   public List<ReplicaReply> handle(List<Request> requests, ReplicaState state) {
//     var readyRequests = new TreeSet<Request>();
//     for (var request : requests) {
//       logger.info(
//           "Got request",
//           new Attr("RID", request.id()),
//           new Attr("type", request.getClass().getSimpleName()));

//       if (request instanceof Request.Single || state.enqueue(request.id())) {
//         readyRequests.add(request);
//       }
//     }
//     System.out.println("ayo");
//     System.out.println(readyRequests.size());

//     // Collect all Single requests
//     var singleRequests = new ArrayList<Request.Single>();
//     for (var request : readyRequests) {
//       if (request instanceof Request.Single) {
//         singleRequests.add((Request.Single) request);
//       } else if (request instanceof Request.Batch) {
//         var batch = (Request.Batch) request;
//         singleRequests.addAll(List.of(batch.requests()));
//       }
//     }

//     var currentGroup = info.groupID();
//     for (int i = 0; i < singleRequests.size(); i++) {
//       var request = singleRequests.get(i);
//       var targetGroups = Arrays.stream(request.targetGroups()).boxed().collect(Collectors.toList());
//       if (!targetGroups.contains(currentGroup)) continue;

//       state.markAsHandled(request);
//       logger.info("Request handled", new Attr("RID", request.id()));
//       targetGroups.remove((Integer) currentGroup);

//       var targetGroupsArray = targetGroups.stream().mapToInt(Integer::intValue).toArray();
//       var requestWithoutCurrentGroup =
//           new Request.Single(request.id(), targetGroupsArray, request.content());

//       singleRequests.set(i, requestWithoutCurrentGroup);
//     }

//     var groupToRequests = new TreeMap<Integer, List<Request.Single>>();
//     for (var request : singleRequests) {
//       var targetGroups = Arrays.stream(request.targetGroups()).boxed().collect(Collectors.toList());
//       if (targetGroups.size() == 0) {
//         continue;
//       }

//       System.out.println("The target groups are " + targetGroups);
//       System.out.println("The current group is " + this.info.groupID());

//       var nextGroups = this.topology.findPaths(this.info.groupID(), targetGroups).get();
//       for (var entry : nextGroups.entrySet()) {
//         var group = entry.getKey();
//         var path = entry.getValue();

//         var requestList = groupToRequests.computeIfAbsent(group, (key) -> new ArrayList<>());
//         var newRequest =
//             new Request.Single(
//                 request.id(),
//                 path.stream().mapToInt(Integer::intValue).toArray(),
//                 request.content());
//         requestList.add(newRequest);
//       }
//     }

//     logger.info("Sending downstream");
//     var futures = new ArrayList<Future<byte[]>>();
//     for (var entry : groupToRequests.entrySet()) {
//       var nextGroup = entry.getKey();
//       var requestsForGroup = entry.getValue();

//       var requestsInBatch = requestsForGroup.toArray(new Request.Single[0]);
//       var batchRequest = new Request.Batch(generateUUID(requestsInBatch), requestsInBatch);
//       System.out.println("The batch request ID is " + batchRequest.id());
//       // var batchRequest =
//       //     new Request.Batch(UUID.randomUUID(), requestsForGroup.toArray(new Request.Single[0]));

//       var proxy = this.proxies.forGroup(nextGroup);
//       var future =
//           executor.submit(
//               () -> {
//                 var payload = Serializer.toBytes(batchRequest);
//                 var reply = proxy.invokeOrdered(payload);
//                 return reply;
//               });
//       futures.add(future);
//     }

//     logger.info("Waiting downstream");
//     var replies = new ArrayList<byte[]>();
//     for (var future : futures) {
//       try {
//         var reply = future.get();
//         replies.add(reply);
//       } catch (Exception e) {
//         throw new RuntimeException("Error while waiting for future", e);
//       }
//     }
//     logger.info("Waited downstream");

//     return requests.stream()
//         .map(
//             request -> {
//               if (readyRequests.contains(request)) {
//                 logger.info("Completing request", new Attr("RID", request.id()));
//                 System.out.println("COMPLETED RAW GET BYTES");
//                 return new ReplicaReply.Completed(request.id(), "not yet implemented".getBytes());
//               }

//               logger.info("Pending request", new Attr("RID", request.id()));
//               return new ReplicaReply.Pending(request.id());
//             })
//         .collect(Collectors.toList());
//   }

//   public List<ReplicaReply> handle2(List<Request> requests, ReplicaState state) {
//     var readyForProcessing = new TreeSet<Request.Single>();
//     for (var request : requests) {
//       if (request instanceof Request.Single) {
//         readyForProcessing.add((Request.Single) request);
//         continue;
//       }

//       var batch = (Request.Batch) request;
//       for (var requestInBatch : batch.requests()) {
//         if (state.enqueue(requestInBatch.id())) {
//           readyForProcessing.add(requestInBatch);
//         }
//       }
//     }

//     var response = processRequests(readyForProcessing, state);

//     var replies = new ArrayList<ReplicaReply>(requests.size());
//     for (var request : requests) {
//       if (request instanceof Request.Single) {
//         if (response.containsKey(request)) {
//           System.out.println("COMPLETED: response.get(request) " + response.get(request));
//           replies.add(new ReplicaReply.Completed(request.id(), response.get(request)));
//         } else {
//           replies.add(new ReplicaReply.Pending(request.id()));
//         }

//         continue;
//       }

//       // assumimos aqui que se a resposta contém um, ela contem todos.
//       // Batch request
//       var batch = (Request.Batch) request;
//       replies.add(replyForBatch(response, batch));
//     }
//     return replies;
//   }

//   private Map<Request.Single, byte[]> processRequests(
//       TreeSet<Request.Single> requests, ReplicaState state) {
//     var requestsInBatch = new ArrayList<>(requests);
//     for (int i = 0; i < requestsInBatch.size(); i++) {
//       var request = requestsInBatch.get(i);

//       var requestTargets =
//           Arrays.stream(request.targetGroups()).boxed().collect(Collectors.toList());
//       if (!requestTargets.contains(this.info.groupID())) continue;

//       state.markAsHandled(request);
//       logger.info("Request marked as handled", new Attr("RID", request.id()));
//       requestTargets.remove((Integer) this.info.groupID());

//       var requestWithoutCurrentGroup =
//           new Request.Single(
//               request.id(),
//               requestTargets.stream().mapToInt(Integer::intValue).toArray(),
//               request.content());

//       requestsInBatch.set(i, requestWithoutCurrentGroup);
//     }

//     var groupToRequests = new TreeMap<Integer, List<Request.Single>>();
//     // var response = new TreeMap<Request.Single, byte[]>();
//     var syncResponse = Collections.synchronizedMap(new TreeMap<Request.Single, byte[]>());

//     for (var request : requestsInBatch) {
//       var targetGroups = Arrays.stream(request.targetGroups()).boxed().collect(Collectors.toList());
//       if (targetGroups.size() == 0) {
//         syncResponse.put(request, "Saul Goodman".getBytes());
//         continue;
//       }

//       var nextGroups = this.topology.findPaths(this.info.groupID(), targetGroups).get();
//       for (var entry : nextGroups.entrySet()) {
//         var group = entry.getKey();
//         var path = entry.getValue();

//         var requestList = groupToRequests.computeIfAbsent(group, (key) -> new ArrayList<>());
//         var newRequest =
//             new Request.Single(
//                 request.id(),
//                 path.stream().mapToInt(Integer::intValue).toArray(),
//                 request.content());
//         requestList.add(newRequest);
//       }
//     }

//     // var batchRequests = new Array

//     var threads = new ArrayList<Thread>();
//     for (var entry : groupToRequests.entrySet()) {
//       var nextGroup = entry.getKey();
//       var requestsForGroup = entry.getValue();

//       var batchRequest =
//           new Request.Batch(UUID.randomUUID(), requestsForGroup.toArray(new Request.Single[0]));
//       var proxy = this.proxies.forGroup(nextGroup);
//       var thread =
//           Thread.ofVirtual()
//               .start(
//                   () -> {
//                     logger.info("Sending batch request to group", new Attr("group", nextGroup));
//                     var payload = Serializer.toBytes(batchRequest);
//                     var reply = proxy.invokeOrdered(payload);
//                     System.out.println("MY REPLY IS " + reply);
//                     for (var req : requestsForGroup) {
//                       syncResponse.put(req, reply);
//                     }
//                   });
//       threads.add(thread);
//     }

//     for (var thread : threads) {
//       try {
//         thread.join();
//       } catch (InterruptedException e) {
//         throw new RuntimeException(e);
//       }
//     }

//     return syncResponse;
//   }

//   private ReplicaReply replyForBatch(Map<Request.Single, byte[]> resps, Request.Batch batch) {
//     for (var request : batch.requests()) {
//       if (resps.containsKey(request) || pending.containsKey(request)) {
//         if (!pending.containsKey(request)) {
//           pending.put(request, resps.get(request));
//         }

//         continue;
//       }

//       System.out.println("PENDING: request " + request);
//       return new ReplicaReply.Pending(batch.id());
//     }

//     var responses = new byte[batch.requests().length][];
//     for (int i = 0; i < batch.requests().length; i++) {
//       responses[i] = pending.get(batch.requests()[i]);
//     }

//     System.out.println("COMPLETED: responses[0] " + responses[0]);
//     return new ReplicaReply.Completed(batch.id(), responses[0]);
//   }

//   private UUID generateUUID(Request.Single[] requestsInBatch) {
//     try {
//       MessageDigest digest = MessageDigest.getInstance("SHA-256");
//       for (Request.Single request : requestsInBatch) {
//         digest.update(Serializer.toBytes(request));
//       }
//       byte[] hash = digest.digest();
//       ByteBuffer byteBuffer = ByteBuffer.wrap(hash);
//       long mostSigBits = byteBuffer.getLong();
//       long leastSigBits = byteBuffer.getLong();
//       return new UUID(mostSigBits, leastSigBits);
//     } catch (Exception e) {
//       throw new RuntimeException("Error generating UUID", e);
//     }
//   }
// }
