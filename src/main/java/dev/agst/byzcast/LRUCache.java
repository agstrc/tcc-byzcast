package dev.agst.byzcast;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public class LRUCache<K, V> implements Serializable {
  private final Map<K, V> cache;

  public LRUCache(int capacity) {
    this.cache =
        new LinkedHashMap<K, V>(capacity, 0.75f, true) {
          @Override
          protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > capacity;
          }
        };
  }

  public void put(K key, V value) {
    cache.put(key, value);
  }

  public V get(K key) {
    return cache.get(key);
  }

  public void remove(K key) {
    cache.remove(key);
  }

  public void clear() {
    cache.clear();
  }

  public int size() {
    return cache.size();
  }

  public boolean containsKey(K key) {
    return cache.containsKey(key);
  }

  public boolean isEmpty() {
    return cache.isEmpty();
  }

  public TreeMap<K, V> getEntries() {
    return new TreeMap<>(cache);
  }
}
