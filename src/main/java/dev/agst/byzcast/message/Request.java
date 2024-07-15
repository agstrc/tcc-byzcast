package dev.agst.byzcast.message;

import java.io.Serializable;
import java.util.UUID;

/**
 * Represents a request within the ByzCast system. This record encapsulates the data necessary for
 * processing a request, including its unique identifier, target groups, content, and source.
 *
 * <p>A {@code Request} object is characterized by the following attributes:
 *
 * <ul>
 *   <li>{@code id} - A {@link UUID} that uniquely identifies the request.
 *   <li>{@code targetGroups} - An array of integers representing the group IDs to which the request
 *       is targeted.
 *   <li>{@code content} - A {@link String} containing the content of the request.
 *   <li>{@code source} - An enumeration value of type {@link Source}, indicating the origin of the
 *       request (CLIENT or REPLICA).
 * </ul>
 *
 * This record implements the {@link Serializable} interface to allow for object serialization,
 * facilitating network transmission or storage.
 */
public record Request(UUID id, int[] targetGroups, String content, Source source)
    implements Serializable {

  /**
   * Enumerates the possible sources of a {@code Request} within the ByzCast system.
   *
   * <p>This enumeration distinguishes between requests originating from clients and those from
   * replicas, allowing the system to appropriately handle each type of request based on its source.
   *
   * <ul>
   *   <li>{@code CLIENT} - Indicates that the request originated from a client.
   *   <li>{@code REPLICA} - Indicates that the request originated from a replica within the ByzCast
   *       system.
   * </ul>
   */
  public static enum Source {
    CLIENT,
    REPLICA
  }
}
