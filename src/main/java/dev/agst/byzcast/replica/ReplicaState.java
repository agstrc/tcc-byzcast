package dev.agst.byzcast.replica;

import dev.agst.byzcast.LRUCache;
import dev.agst.byzcast.message.Request;
import dev.agst.byzcast.message.Response;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Optional;
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
  // note: the BFT-SMaRt library has the following point in its README:
  // https://github.com/bft-smart/library/blob/b23a3a1f1abcac54a74f5771c70e73c64b138e9b/README.md
  // Important tip #8: Regardless of the chosen protocol, developers must avoid using Java API
  // objects like HashSet or HashMap, and use TreeSet or TreeMap instead. This is because
  // serialization of Hash* objects is not deterministic, i.e, it generates different byte arrays
  // for equal objects. This will lead to problems after more than f replicas used the state
  // transfer protocol to recover from failures.
  //
  // I haven't quite tested it out, but I'd assume that HashMaps present this behavior due to
  // iteration order being non-deterministic. Therefore, by using LinkedHashMap in this class
  // (including the LRU cache implementation), I assume this issue does not apply. However, I
  // haven't tested this out.

  /**
   * A set to keep track of handled requests to ensure that a request is processed only once. This
   * is currently a placeholder for the business logic that could be implemented in the future.
   */
  private final LinkedHashSet<Request> handled = new LinkedHashSet<>();

  /**
   * A map to keep track of the number of times a request has been received. This is used to
   * determine when a request has met the minimum receive count and is ready to be processed.
   */
  private final LinkedHashMap<UUID, Integer> pending = new LinkedHashMap<>();

  /**
   * A cache for storing responses to requests. This LRU (Least Recently Used) cache is used to
   * quickly retrieve responses for requests that have been processed before, thus avoiding
   * reprocessing of the same request.
   */
  private final LRUCache<UUID, Response> cache = new LRUCache<>(2056);

  /**
   * The minimum number of times a request must be received before it is considered ready for
   * processing. This threshold is based on ByzCast parameters, specifically the formula N-F, where
   * N is the total number of replicas and F is the maximum number of faulty replicas the system can
   * tolerate. This ensures that a request is only processed when it has been received from a
   * sufficient number of replicas to guarantee consensus in the presence of faults.
   */
  private final int minReceiveCount;

  public ReplicaState(int minReceiveCount) {
    this.minReceiveCount = minReceiveCount;
  }

  public Optional<Response> getCachedResponse(UUID id) {
    return Optional.ofNullable(cache.get(id));
  }

  /**
   * Sets a request as pending and returns whether the request has reached the minimum number of
   * receives required to be processed.
   */
  public boolean enqueue(Request request) {
    var pendingTotal = this.pending.compute(request.id(), (k, v) -> (v == null) ? 1 : v + 1);
    return pendingTotal == minReceiveCount;
  }

  public void cacheResponse(Request request, Response response) {
    cache.put(request.id(), response);
    pending.remove(request.id());
  }

  public void markAsHandled(Request request) {
    handled.add(request);
  }
}
