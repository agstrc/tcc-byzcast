package dev.agst.byzcast.topology;

/** Exception thrown when there is an error loading a topology from a file. */
public class TopologyLoadException extends Exception {
  TopologyLoadException(Throwable cause) {
    super(cause);
  }
}
