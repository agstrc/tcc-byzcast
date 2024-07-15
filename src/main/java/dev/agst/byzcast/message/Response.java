package dev.agst.byzcast.message;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Represents a response from a ByzCast node.
 *
 * <p>This record encapsulates the essential elements of a response, including its content and a
 * list of {@code GroupResponse} objects. Each {@code GroupResponse} object signifies a distinct
 * response received from an upstream group, uniquely identified by its group ID.
 *
 * <p>The {@code Response} record is composed of the following attributes:
 *
 * <ul>
 *   <li>{@code content} - A {@link String} representing the main content of the response.
 *   <li>{@code responses} - An {@link ArrayList} of {@code GroupResponse} objects, each
 *       representing a response from an individual group within the network.
 * </ul>
 *
 * <p>This record implements the {@link Serializable} interface, enabling it to be serialized for
 * network transmission or persistent storage.
 */
public record Response(String content, ArrayList<GroupResponse> responses) implements Serializable {
  public static record GroupResponse(int groupID, Response response) implements Serializable {}
}
