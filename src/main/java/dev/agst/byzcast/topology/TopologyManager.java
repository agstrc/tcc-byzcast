package dev.agst.byzcast.topology;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manages the topology state in a thread-safe manner for the ByzCast system.
 *
 * <p>This class is responsible for:
 *
 * <ul>
 *   <li>Holding the current {@link Topology} reference using thread-safe atomic operations
 *   <li>Managing {@link TopologyState} transitions during updates
 *   <li>Providing safe access to the topology from multiple threads
 * </ul>
 *
 * <p>The manager ensures that during topology updates, the state is properly transitioned to
 * {@link TopologyState#UPDATING} to reject incoming requests, and then back to {@link
 * TopologyState#READY} after successful update or {@link TopologyState#ERROR} on failure.
 */
public class TopologyManager {
  private final AtomicReference<Topology> topology;
  private final AtomicReference<TopologyState> state;
  private final AtomicReference<String> lastError;

  /**
   * Creates a new TopologyManager with the given initial topology.
   *
   * @param initialTopology The initial topology to use. Must not be null.
   */
  public TopologyManager(Topology initialTopology) {
    if (initialTopology == null) {
      throw new IllegalArgumentException("Initial topology must not be null");
    }
    this.topology = new AtomicReference<>(initialTopology);
    this.state = new AtomicReference<>(TopologyState.READY);
    this.lastError = new AtomicReference<>(null);
  }

  /**
   * Returns the current topology.
   *
   * @return The current {@link Topology} instance.
   */
  public Topology getTopology() {
    return topology.get();
  }

  /**
   * Returns the current state of the topology manager.
   *
   * @return The current {@link TopologyState}.
   */
  public TopologyState getState() {
    return state.get();
  }

  /**
   * Returns the last error message if the state is ERROR.
   *
   * @return The last error message, or null if no error occurred.
   */
  public String getLastError() {
    return lastError.get();
  }

  /**
   * Updates the topology with a new configuration.
   *
   * <p>This method:
   *
   * <ol>
   *   <li>Sets the state to UPDATING
   *   <li>Builds the new topology from the provided list
   *   <li>Replaces the current topology
   *   <li>Sets the state back to READY on success, or ERROR on failure
   * </ol>
   *
   * @param newTopologyList The new topology configuration as a list of lists where each list
   *     represents a group and its children.
   * @throws TopologyUpdateException If the topology update fails.
   */
  public synchronized void updateTopology(List<List<Integer>> newTopologyList)
      throws TopologyUpdateException {
    state.set(TopologyState.UPDATING);
    lastError.set(null);

    try {
      var root = TopologyLoader.buildFromList(newTopologyList);
      var newTopology = new Topology(root);
      topology.set(newTopology);
      state.set(TopologyState.READY);
    } catch (Exception e) {
      lastError.set(e.getMessage());
      state.set(TopologyState.ERROR);
      throw new TopologyUpdateException("Failed to update topology: " + e.getMessage(), e);
    }
  }

  /**
   * Resets the state from ERROR back to READY. This is used after an error has been acknowledged.
   *
   * @return true if the state was ERROR and has been reset to READY, false otherwise.
   */
  public boolean acknowledgeError() {
    return state.compareAndSet(TopologyState.ERROR, TopologyState.READY);
  }

  /**
   * Checks if the topology manager is ready to accept requests.
   *
   * @return true if the state is READY, false otherwise.
   */
  public boolean isReady() {
    return state.get() == TopologyState.READY;
  }
}
