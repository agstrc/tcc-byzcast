package dev.agst.byzcast.message;

import java.io.Serializable;
import java.util.UUID;

public record Request(UUID id, int[] targetGroups, boolean isForwarded, String content)
    implements Serializable, Comparable<Request> {

  @Override
  public int compareTo(Request o) {
    return this.id.compareTo(o.id);
  }
}
