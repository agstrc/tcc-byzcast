package dev.agst.byzcast.server;

import dev.agst.byzcast.LRUCache;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

public class ServerState implements Serializable {
  private final ArrayList<UUID> handled = new ArrayList<>();

  private final TreeMap<UUID, Integer> pending = new TreeMap<>();

  private final LRUCache<UUID, byte[]> cache = new LRUCache<>(2056 * 1_000);

  private final int minReceiveCount;

  public ServerState(int minReceiveCount) {
    this.minReceiveCount = minReceiveCount;
  }

  public Optional<byte[]> getCachedResponse(UUID id) {
    return Optional.ofNullable(cache.get(id));
  }

  public boolean enqueue(UUID id) {
    var pendingTotal = this.pending.compute(id, (k, v) -> (v == null) ? 1 : v + 1);
    return pendingTotal >= minReceiveCount;
  }

  public void cacheResponse(UUID id, byte[] response) {
    cache.put(id, response);
    pending.remove(id);
  }

  public void markAsHandled(UUID id) {
    handled.add(id);
  }
}
