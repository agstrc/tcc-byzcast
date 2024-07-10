package dev.agst.byzcast.message;

import java.io.Serializable;
import java.util.UUID;

/**
 * Represents a request within the ByzCast system. This class encapsulates the data for a request
 * made by clients, including a unique identifier, the content of the request, and the target group
 * ID the request is intended for.
 *
 * <p>Each request is serializable, which is the implementation for how the request is sent over the
 * network.
 */
public class Request implements Serializable {
  private final UUID id = UUID.randomUUID();

  private final String content;
  private final int targetGroupID;

  /**
   * Constructs a new Request with the specified content and target group ID.
   *
   * @param content The content of the request.
   * @param targetGroupID The ID of the target group intended to receive the request.
   */
  public Request(String content, int targetGroupID) {
    this.content = content;
    this.targetGroupID = targetGroupID;
  }

  /**
   * Returns the unique identifier of this request.
   *
   * @return The UUID of the request.
   */
  public UUID getID() {
    return id;
  }

  /**
   * Returns the content of this request.
   *
   * @return The content as a String.
   */
  public String getContent() {
    return content;
  }

  /**
   * Returns the target group ID for this request.
   *
   * @return The target group ID as an integer.
   */
  public int getTargetGroupID() {
    return targetGroupID;
  }

  /**
   * Creates a Request object from a byte array. This method is used for deserializing a request
   * from bytes.
   *
   * @param bytes The byte array to deserialize the request from.
   * @return A Request object.
   * @throws MessageDeserializationException If the byte array does not represent a valid Request.
   */
  public static Request fromBytes(byte[] bytes) throws MessageDeserializationException {
    return Serializer.requestFromBytes(bytes);
  }

  /**
   * Serializes this Request to a byte array. This method is used for serializing the request for
   * network transmission or storage.
   *
   * @return A byte array representing this Request.
   */
  public byte[] toBytes() {
    return Serializer.toBytes(this);
  }
}
