package dev.agst.byzcast.topology;

/**
 * Exception thrown when a topology update operation fails.
 *
 * <p>This exception is thrown by {@link TopologyManager#updateTopology} when the provided topology
 * configuration is invalid or cannot be applied.
 */
public class TopologyUpdateException extends Exception {

  /**
   * Constructs a new TopologyUpdateException with the specified message.
   *
   * @param message The detail message.
   */
  public TopologyUpdateException(String message) {
    super(message);
  }

  /**
   * Constructs a new TopologyUpdateException with the specified message and cause.
   *
   * @param message The detail message.
   * @param cause The cause of this exception.
   */
  public TopologyUpdateException(String message, Throwable cause) {
    super(message, cause);
  }
}
