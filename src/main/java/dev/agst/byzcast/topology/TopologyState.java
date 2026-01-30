package dev.agst.byzcast.topology;

/**
 * Represents the state of the topology in the ByzCast system.
 *
 * <p>This enum is used to control request processing during topology updates. When the topology is
 * being updated, incoming requests are rejected to ensure consistency across all replicas.
 */
public enum TopologyState {
  /**
   * Normal operation state. The system is ready to accept and process requests. This is the
   * initial state after startup and the state after a successful topology update.
   */
  READY,

  /**
   * Topology update in progress. All incoming requests will be rejected with a
   * TOPOLOGY_UPDATING response until the update is complete.
   */
  UPDATING,

  /**
   * Topology update failed. Requests will be rejected with a TOPOLOGY_ERROR response. The system
   * will transition back to READY after error acknowledgment or timeout.
   */
  ERROR
}
