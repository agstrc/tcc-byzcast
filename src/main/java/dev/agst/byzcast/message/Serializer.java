package dev.agst.byzcast.message;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Offers utilities for the serialization and deserialization of {@link Request} and {@link
 * Response} objects within the ByzCast system. These utilities enable the conversion of {@link
 * Request} and {@link Response} objects to and from byte arrays, facilitating their transmission
 * over the network.
 */
class Serializer {
  /**
   * Converts any given object into a byte array. This method serves as the implementation of
   * serialization of {@link Request} and {@link Response} objects.
   */
  static byte[] toBytes(Object object) {
    try {
      var arrayStream = new ByteArrayOutputStream();
      var objectStream = new ObjectOutputStream(arrayStream);

      objectStream.writeObject(object);
      objectStream.flush();
      return arrayStream.toByteArray();
    } catch (IOException e) {
      // we are writing into an in-memory byte array, so this should never happen
      throw new RuntimeException(e);
    }
  }

  /**
   * Deserializes a {@link Request} object from a byte array. This method is used for creating a
   * request instance from bytes.
   *
   * @param bytes The byte array to deserialize the request from.
   * @return A {@link Request} object.
   * @throws MessageDeserializationException If the byte array does not represent a valid {@link
   *     Request}.
   */
  static Request requestFromBytes(byte[] bytes) throws MessageDeserializationException {
    return fromBytes(bytes, Request.class);
  }

  /**
   * Deserializes a {@link Response} object from a byte array. This method is used for creating a
   * response instance from bytes.
   *
   * @param bytes The byte array to deserialize the response from.
   * @return A {@link Response} object.
   * @throws MessageDeserializationException If the byte array does not represent a valid {@link
   *     Response}.
   */
  static Response responseFromBytes(byte[] bytes) throws MessageDeserializationException {
    return fromBytes(bytes, Response.class);
  }

  /**
   * Deserialize the given byte array into an object of the specified type.
   *
   * @param bytes the byte array to deserialize
   * @param type the class of the object to be deserialized
   * @param <T> the type of the object to be deserialized
   * @return the deserialized object
   * @throws MessageDeserializationException if an error occurs during deserialization
   */
  private static <T> T fromBytes(byte[] bytes, Class<T> type)
      throws MessageDeserializationException {
    Object acquiredObject;

    try {
      var arrayStream = new ByteArrayInputStream(bytes);
      var objectStream = new ObjectInputStream(arrayStream);

      acquiredObject = objectStream.readObject();
      if (type.isInstance(acquiredObject)) {
        return type.cast(acquiredObject);
      }
    } catch (Exception e) {
      throw new MessageDeserializationException(e);
    }

    throw new MessageDeserializationException(type, acquiredObject.getClass());
  }
}
