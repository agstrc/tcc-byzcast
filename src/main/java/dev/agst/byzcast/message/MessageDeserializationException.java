package dev.agst.byzcast.message;

/**
 * Represents an exception that occurs during the deserialization process of data into specific
 * types. This exception is specifically thrown when there is a failure in converting serialized
 * data back into either a {@link Request} or {@link Response} object, or when the deserialized object
 * does not match the expected types.
 */
public class MessageDeserializationException extends Exception {
  /**
   * Constructs a new exception with the specified cause.
   *
   * @param e the cause of the exception.
   */
  MessageDeserializationException(Exception e) {
    super(e);
  }

  /**
   * Constructs a new exception with a detailed message indicating a mismatch between the expected
   * type and the actual type of the deserialized object.
   *
   * @param expectedType the {@code Class} object representing the expected type to which the data
   *     should have been deserialized.
   * @param actualType the {@code Class} object representing the actual type of the deserialized
   *     object. This constructor is used when a valid Java object is deserialized, but it does not
   *     match the expected types of {@link Request} or {@link Response}.
   */
  MessageDeserializationException(Class<?> expectedType, Class<?> actualType) {
    super(
        "Invalid message type: expected "
            + expectedType.getName()
            + ", but got "
            + actualType.getName());
  }
}
