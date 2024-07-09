package dev.agst.byzcast.group;

/** This exception is thrown when there is an error loading the {@link GroupMap} */
public class GroupMapLoadException extends Exception {
  /**
   * Constructs a new GroupMapLoadException with the specified cause.
   *
   * @param e the cause of the exception
   */
  public GroupMapLoadException(Exception e) {
    super(e);
  }
}
