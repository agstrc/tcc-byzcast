package dev.agst.byzcast.topology;

/** TopologyLoadException is thrown when there is an error loading a topology from a file. */
public class TopologyLoadException extends Exception {
  public TopologyLoadException(Throwable cause) {
    super(cause);
  }
}
