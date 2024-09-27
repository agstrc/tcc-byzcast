package dev.agst.byzcast.v2.message;

import java.io.Serializable;
import java.util.ArrayList;

public sealed interface Response extends Serializable {
  public record Single(String content, ArrayList<GroupResponse> responses) implements Response {
    public static record GroupResponse(int groupID, Response response) implements Serializable {}
  }

  public record Batch(Single[] responses) implements Response {}
}
