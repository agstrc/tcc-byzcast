package dev.agst.byzcast.message;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * Represents a request to update the topology in the ByzCast system.
 *
 * <p>This record is used to propagate topology updates through BFT-SMaRT consensus to ensure all
 * replicas apply the update consistently and in the same order.
 *
 * <p>The topology is represented as a list of lists, where each inner list contains the children
 * group IDs for the group at that index. For example:
 *
 * <pre>{@code
 * [
 *   [1, 2],  // Group 0 has children 1 and 2
 *   [3],     // Group 1 has child 3
 *   [],      // Group 2 has no children
 *   []       // Group 3 has no children
 * ]
 * }</pre>
 *
 * @param id A unique identifier for this update request.
 * @param topology The new topology configuration as a list of lists.
 */
public record TopologyUpdateRequest(UUID id, List<List<Integer>> topology) implements Serializable {

  /**
   * Creates a new TopologyUpdateRequest with a generated UUID.
   *
   * @param topology The new topology configuration.
   * @return A new TopologyUpdateRequest with a random UUID.
   */
  public static TopologyUpdateRequest create(List<List<Integer>> topology) {
    return new TopologyUpdateRequest(UUID.randomUUID(), topology);
  }
}
