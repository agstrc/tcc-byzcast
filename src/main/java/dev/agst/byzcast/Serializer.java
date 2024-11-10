package dev.agst.byzcast;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

/**
 * The {@code Serializer} class provides static methods for serializing objects to byte arrays and
 * deserializing byte arrays back into objects. It is designed to work with any objects that
 * implement the {@link Serializable} interface.
 */
public class Serializer {

  /**
   * Deserializes a byte array back into an object of the specified type.
   *
   * @param <T> the type of the object to be deserialized
   * @param bytes the byte array to deserialize
   * @param desiredType the {@link Class} object representing the type of the object to be
   *     deserialized
   * @return an object of the specified type
   * @throws SerializingException if deserialization fails due to an I/O error, class not found, or
   *     class cast exception
   */
  public static <T> T fromBytes(byte[] bytes, Class<T> desiredType) throws SerializingException {
    try {
      var bytesStream = new ByteArrayInputStream(bytes);
      var objectStream = new ObjectInputStream(bytesStream);

      var obj = objectStream.readObject();
      return desiredType.cast(obj);
    } catch (IOException | ClassNotFoundException | ClassCastException e) {
      var exception =
          new SerializingException(
              String.format("Failed to serialize bytes to type %s", desiredType.getName()), e);
      exception.addSuppressed(e);
      throw exception;
    }
  }

  /**
   * Serializes an object into a byte array.
   *
   * @param <T> the type of the object to be serialized, must implement {@link Serializable}
   * @param obj the object to serialize
   * @return a byte array representing the serialized object
   * @throws RuntimeException if the serialization fails due to a class that should not be
   *     serialized
   */
  public static <T extends Serializable> byte[] toBytes(T obj) {
    try {
      var bytesStream = new java.io.ByteArrayOutputStream();
      var objectStream = new java.io.ObjectOutputStream(bytesStream);

      objectStream.writeObject(obj);
      objectStream.flush();
      return bytesStream.toByteArray();
    } catch (IOException e) {
      // we are writing to a byte array, so in general this should not happen. But as documented
      // in the ObjectOutputStream class, it can happen if the object being serialized is not
      // serializable.
      throw new RuntimeException("Failed to serialize object", e);
    }
  }
}
