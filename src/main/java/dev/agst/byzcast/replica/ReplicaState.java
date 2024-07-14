package dev.agst.byzcast.replica;

import dev.agst.byzcast.LRUCache;
import dev.agst.byzcast.message.Request;
import dev.agst.byzcast.message.Response;
import java.io.Serializable;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.TreeSet;
import java.util.UUID;

/**
 * This class is responsible for managing the state of a single replica within the ByzCast
 * distributed system. It plays a crucial role in handling the coordination and communication
 * between replicas by:
 *
 * <p>1. Storing incoming requests from other replicas until they accumulate to a predefined
 * threshold (N-F), where N is the total number of replicas and F is the maximum number of faulty
 * replicas the system can tolerate.
 *
 * <p>2. Caching the responses to these requests. This cache prevents the need for reprocessing a
 * request if additional replicas send the same request after the threshold has been reached.
 *
 * @see dev.agst.byzcast.replica.ReplicaReplier
 */
public class ReplicaState implements Serializable {
  private final LRUCache<UUID, Response> repliesCache = new LRUCache<>(2056);
  private final LinkedHashMap<UUID, Integer> pendingRequests = new LinkedHashMap<>();
  private final TreeSet<Request> handledRequets = new TreeSet<>(new RequestComparator());

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

  private static class RequestComparator implements Serializable, Comparator<Request> {
    @Override
    public int compare(Request r1, Request r2) {
      return r1.id().compareTo(r2.id());
    }
  }
}
