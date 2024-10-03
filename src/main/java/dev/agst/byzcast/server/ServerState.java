package dev.agst.byzcast.server;

import dev.agst.byzcast.message.Response.AggregatedResponse;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Maintains the state of a single ByzCast node.
 *
 * <p>This class tracks the state of a ByzCast node, including handled requests, pending requests,
 * and cached responses. It ensures that requests are processed and responded to correctly based on
 * the protocol's requirements.
 */
public class ServerState implements Serializable {

  /**
   * List of IDs of all requests that have been handled by the group.
   *
   * <p>This list serves as a placeholder for real application logic to track which requests have
   * been processed.
   */
  private ArrayList<UUID> handledRequests = new ArrayList<>();

  /**
   * Maps the IDs of requests received from other replicas to the number of times they have been
   * received.
   *
   * <p>This map is used to control when a request has been received N-F times, so that it may be
   * handled.
   */
  private final TreeMap<UUID, Integer> pendingRequests = new TreeMap<>();

  /**
   * Caches the number of times a given request has been replied to.
   *
   * <p>This cache helps ensure that a request is not re-executed once (N-F) + 1 requests have
   * arrived and keeps the request in cache until the last replica requires it.
   */
  private final TreeMap<UUID, ResponseCacheEntry> responseCache = new TreeMap<>();

  /** The minimum number of times a request must be received before it can be processed. */
  private final int minReceiveCount;

  /** The maximum number of times a response can be returned before it is removed from the cache. */
  private final int maxReplyCount;

  /**
   * Constructs a new {@code ServerState} with the specified parameters.
   *
   * @param minReceiveCount The minimum number of times a request must be received before it can be
   *     processed.
   * @param maxReplyCount The maximum number of times a response can be returned before it is
   *     removed from the cache.
   */
  public ServerState(int minReceiveCount, int maxReplyCount) {
    this.minReceiveCount = minReceiveCount;
    this.maxReplyCount = maxReplyCount;
  }

  /**
   * Retrieves a cached response for the specified request ID.
   *
   * <p>This method returns the cached {@link AggregatedResponse} for the given request ID, if it
   * exists. The response is not removed from the cache regardless of how many times it has been
   * returned. To manage the return count and potentially remove the response from the cache, use
   * the {@link #increaseReturnCount(UUID)} method.
   *
   * @param id The unique identifier of the request.
   * @return An {@code Optional} containing the cached {@link AggregatedResponse}, or an empty
   *     {@code Optional} if no response is cached.
   */
  public Optional<AggregatedResponse> getCachedResponse(UUID id) {
    return Optional.ofNullable(responseCache.get(id)).map(ResponseCacheEntry::response);
  }

  /**
   * Increases the return count for the specified request ID.
   *
   * <p>This method increments the return count for the cached response associated with the given
   * request ID. If the return count reaches {@code maxReplyCount}, the response is removed from the
   * cache.
   *
   * @param id The unique identifier of the request.
   */
  public void increaseReturnCount(UUID id) {
    var count =
        responseCache.computeIfPresent(
            id, (k, v) -> new ResponseCacheEntry(v.response(), v.returnCount() + 1));
    if (count != null && count.returnCount() >= maxReplyCount) {
      responseCache.remove(id);
    }
  }

  /**
   * Caches a response for the specified request ID.
   *
   * <p>The response is stored in the cache and the request is removed from the pending requests.
   *
   * @param id The unique identifier of the request.
   * @param response The {@link AggregatedResponse} to cache.
   */
  public void setCachedResponse(UUID id, AggregatedResponse response) {
    responseCache.put(id, new ResponseCacheEntry(response, 0));
    pendingRequests.remove(id);
  }

  /**
   * Enqueues a request for processing.
   *
   * <p>The request is added to the pending requests and its receive count is incremented. If the
   * receive count reaches {@code minReceiveCount}, the request is ready to be processed.
   *
   * @param id The unique identifier of the request.
   * @return {@code true} if the request is ready to be processed, {@code false} otherwise.
   */
  public boolean enqueue(UUID id) {
    var pendingTotal = pendingRequests.compute(id, (k, v) -> (v == null) ? 1 : v + 1);
    return pendingTotal >= minReceiveCount;
  }

  /**
   * Marks a request as handled.
   *
   * <p>The request ID is added to the list of handled requests.
   *
   * @param id The unique identifier of the request.
   */
  public void markAsHandled(UUID id) {
    handledRequests.add(id);
  }

  /**
   * Represents an entry in the response cache.
   *
   * <p>This record stores the cached response and the number of times it has been returned.
   *
   * @param response The cached {@link AggregatedResponse}.
   * @param returnCount The number of times the response has been returned.
   */
  private static record ResponseCacheEntry(AggregatedResponse response, int returnCount) {}
}
