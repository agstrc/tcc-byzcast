package dev.agst.byzcast.message;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * Represents a response in the ByzCast system. This class encapsulates the response data, including
 * the content of the response and the IDs of the groups through which this response has passed.
 *
 * <p>Each response is serializable, which is the implementation for how the response is sent over
 * the network.
 */
public class Response implements Serializable {
  private final String content;

  private final ArrayList<Integer> groupIDs = new ArrayList<>();

  /**
   * Constructs a new Response with the specified content and the initial group ID.
   *
   * @param content The content of the response.
   * @param currentGroupID The initial group ID from which the response originates.
   */
  public Response(String content, int currentGroupID) {
    this.content = content;
    this.groupIDs.add(currentGroupID);
  }

  /**
   * Returns the content of this response.
   *
   * @return The content as a String.
   */
  public String getContent() {
    return content;
  }

  /**
   * Returns the list of group IDs that are targeted or affected by this response.
   *
   * @return An ArrayList of Integer containing the group IDs.
   */
  public ArrayList<Integer> getGroupIDs() {
    return groupIDs;
  }

  /**
   * Deserializes a Response object from a byte array. This method is used for creating a response
   * instance from bytes.
   *
   * @param bytes The byte array to deserialize the response from.
   * @return A Response object.
   * @throws MessageDeserializationException If the byte array does not represent a valid Response.
   */
  public static Response fromBytes(byte[] bytes) throws MessageDeserializationException {
    return Serializer.responseFromBytes(bytes);
  }

  /**
   * Serializes this Response to a byte array. This method is used for serializing the response for
   * network transmission or storage.
   *
   * @return A byte array representing this Response.
   */
  public byte[] toBytes() {
    return Serializer.toBytes(this);
  }

  /**
   * Adds a new group ID to the list of targeted or affected groups for this response.
   *
   * @param groupID The group ID to add.
   */
  public void addTargetGroupID(int groupID) {
    groupIDs.add(groupID);
  }

  /**
   * Generates a JSON representation of this Response, including its content and group IDs, with
   * pretty printing.
   *
   * @return A String containing the JSON representation of this Response.
   */
  @Override
  public String toString() {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    return gson.toJson(this);
  }
}
