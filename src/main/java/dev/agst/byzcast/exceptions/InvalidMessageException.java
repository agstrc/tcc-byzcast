package dev.agst.byzcast.exceptions;

/** This exception is thrown when a deserialized object is not of type Message. */
public class InvalidMessageException extends Exception {
  public InvalidMessageException(String message) {
    super(message);
  }
}
