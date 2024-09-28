package dev.agst.byzcast.server;

import dev.agst.byzcast.LRUCache;
import dev.agst.byzcast.message.Response.AggregatedResponse;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

public class ServerState implements Serializable {
  private final List<UUID> handled = new ArrayList<>();

  private final TreeMap<UUID, Integer> pending = new TreeMap<>();

  /*
   * an arbritrary capacity is chosen. Ideally, the response cache should
   * invalidate responses once a total of N requests for a given ID are restored, or
   * with some timeout in case the N is not reached. But this is not yet implemented.
   */
  private static final int CACHE_CAPACITY = 4096 * 1_000;

  private final LRUCache<UUID, AggregatedResponse> responses = new LRUCache<>(CACHE_CAPACITY);

  private final int minReceiveCount;

  public ServerState(int minReceiveCount) {
    this.minReceiveCount = minReceiveCount;
  }

  public Optional<AggregatedResponse> getCachedResponse(UUID id) {
    return Optional.ofNullable(responses.get(id));
  }

  public void cacheResponse(UUID id, AggregatedResponse response) {
    responses.put(id, response);
    pending.remove(id);
  }

  public boolean enqueue(UUID id) {
    var pendingTotal = this.pending.compute(id, (k, v) -> (v == null) ? 1 : v + 1);
    return pendingTotal >= minReceiveCount;
  }

  public void markAsHandled(UUID id) {
    handled.add(id);
  }
}
