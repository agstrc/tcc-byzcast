package dev.agst.byzcast.message;

import java.io.Serializable;
import java.util.ArrayList;

public sealed interface Response extends Serializable {
  public static record AggregatedResponse(
      String content, ArrayList<GroupResponse> downstreamResponses) implements Response {}

  public static record GroupResponse(int groupID, Response response)
      implements Response, Comparable<GroupResponse> {

    @Override
    public int compareTo(GroupResponse o) {
      return Integer.compare(groupID, o.groupID);
    }
  }

  public static record BatchResponse(ArrayList<AggregatedResponse> responses) implements Response {}
}
