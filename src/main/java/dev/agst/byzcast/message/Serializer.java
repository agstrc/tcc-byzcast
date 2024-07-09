package dev.agst.byzcast.message;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

class Serializer {
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

  static Request requestFromBytes(byte[] bytes) throws MessageDeserializationException {
    return fromBytes(bytes, Request.class);
  }

  static Response responseFromBytes(byte[] bytes) throws MessageDeserializationException {
    return fromBytes(bytes, Response.class);
  }

  private static <T> T fromBytes(byte[] bytes, Class<T> type) throws MessageDeserializationException {
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
