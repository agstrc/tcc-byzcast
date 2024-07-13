package dev.agst.byzcast.message;

import java.io.Serializable;

public record Response(String content, int[] groupIDs)
    implements Serializable, Comparable<Response> {

  @Override
  public int compareTo(Response o) {
    // First, compare the content
    int contentComparison = this.content.compareTo(o.content);
    if (contentComparison != 0) {
      return contentComparison;
    }

    // If contents are equal, compare the groupIDs arrays
    int minLength = Math.min(this.groupIDs.length, o.groupIDs.length);
    for (int i = 0; i < minLength; i++) {
      if (this.groupIDs[i] != o.groupIDs[i]) {
        return Integer.compare(this.groupIDs[i], o.groupIDs[i]);
      }
    }
    // If one array is a prefix of the other, the shorter array is considered less
    return Integer.compare(this.groupIDs.length, o.groupIDs.length);
  }
}
