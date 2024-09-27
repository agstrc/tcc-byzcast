package dev.agst.byzcast.v2.replica;

import dev.agst.byzcast.LRUCache;
import dev.agst.byzcast.v2.message.Request;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.UUID;

public class ReplicaState implements Serializable {
  private final LinkedHashSet<Request> handled = new LinkedHashSet<>();
  private final LinkedHashMap<UUID, Integer> pending = new LinkedHashMap<>();
  private final LRUCache<UUID, byte[]> cache = new LRUCache<>(2056);
  private final int minReceiveCount;

  public ReplicaState(int minReceiveCount) {
    this.minReceiveCount = minReceiveCount;
  }

  public Optional<byte[]> getCachedResponse(UUID id) {
    return Optional.ofNullable(cache.get(id));
  }

  public boolean enqueue(UUID id) {
    var pendingTotal = this.pending.compute(id, (k, v) -> (v == null) ? 1 : v + 1);
    var shouldHandle = pendingTotal >= minReceiveCount;
    if (!shouldHandle) {
      System.out.println(
          "DEBUG: ReplicaState.enqueue: id="
              + id
              + ", pendingTotal="
              + pendingTotal
              + ", minReceiveCount="
              + minReceiveCount);
    }

    return shouldHandle;
  }

  public void cacheResponse(UUID id, byte[] response) {
    cache.put(id, response);
    pending.remove(id);
  }

  public void markAsHandled(Request request) {
    handled.add(request);
  }
}
