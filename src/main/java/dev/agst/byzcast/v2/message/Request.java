package dev.agst.byzcast.v2.message;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;

public sealed interface Request extends Serializable {
  public record Single(UUID id, ArrayList<Integer> targetGroups, String content)
      implements Request, Comparable<Single> {

    @Override
    public int compareTo(Single o) {
      return id.compareTo(o.id);
    }
  }

  public record Batch(ArrayList<Single> requests) implements Request {}
}
