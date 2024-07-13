package dev.agst.byzcast.replica;

import dev.agst.byzcast.LRUCache;
import dev.agst.byzcast.message.Request;
import dev.agst.byzcast.message.Response;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Represents the state of a replica in the Byzantine Broadcast protocol. This class stores the
 * necessary data structures and parameters used by the replica.
 */
public class ReplicaState implements Serializable {
  private final LRUCache<UUID, Response> repliesCache = new LRUCache<>(2056);
  private final LinkedHashMap<UUID, Integer> pendingRequests = new LinkedHashMap<>();
  private final TreeSet<Request> handledRequets = new TreeSet<>();

  private final int targetRequestCount;

  public ReplicaState(int targetRequestCount) {
    this.targetRequestCount = targetRequestCount;
  }

  public Optional<Response> getReply(UUID id) {
    return Optional.ofNullable(repliesCache.get(id));
  }

  public boolean increasePendingRequest(UUID id) {
    var currentPending =
        pendingRequests.compute(
            id,
            (k, v) -> {
              if (v == null) {
                return 1;
              } else {
                return v + 1;
              }
            });

    if (currentPending == targetRequestCount) {
      return true;
    }
    return false;
  }

  public void cacheReply(Request request, Response response) {
    repliesCache.put(request.id(), response);
    pendingRequests.remove(request.id());
    handledRequets.add(request);
  }

  public void addHandledRequest(Request request) {
    handledRequets.add(request);
  }
}
