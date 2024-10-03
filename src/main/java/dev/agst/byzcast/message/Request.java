package dev.agst.byzcast.message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Represents a request within the ByzCast protocol.
 *
 * <p>This sealed interface defines the structure of requests that can be sent within the ByzCast
 * system. Requests can be either {@link ClientRequest} or {@link ReplicaRequest}. Requests sent by
 * replicas are always batched.
 */
public sealed interface Request extends Serializable {

  /**
   * Represents a client request within the ByzCast protocol.
   *
   * <p>This record encapsulates the details of a client request, including a unique identifier,
   * target groups, and the content of the request.
   */
  public static record ClientRequest(UUID id, ArrayList<Integer> targetGroups, String content)
      implements Request {}

  /**
   * Represents a replica request within the ByzCast protocol.
   *
   * <p>This record encapsulates a batch of {@link ClientRequest} instances that are sent by
   * replicas. Since replicas only forward requests originally sent by clients, each {@code
   * ReplicaRequest} is essentially a collection of {@code ClientRequest} instances that have been
   * batched together for efficiency.
   */
  public static record ReplicaRequest(ArrayList<ClientRequest> requests) implements Request {

    /**
     * Generates a unique identifier for the {@code ReplicaRequest} based on the UUIDs of the {@code
     * ClientRequest} instances contained within it.
     *
     * <p>This method concatenates the byte representations of the UUIDs of all {@code
     * ClientRequest} instances in the {@code requests} list and generates a new UUID from the
     * resulting byte array. This ensures that the {@code ReplicaRequest} has a unique identifier
     * that is derived from its constituent {@code ClientRequest} instances.
     *
     * <p>This method guarantees that if the same list of {@code ClientRequest} IDs is provided, the
     * same {@code UUID} will be returned, ensuring consistency in the identification of {@code
     * ReplicaRequest} instances.
     *
     * @return a {@code UUID} that uniquely identifies this {@code ReplicaRequest}
     */
    public UUID id() {
      try (var stream = new ByteArrayOutputStream()) {
        for (var request : this.requests()) {
          var id = request.id();
          var buffer = ByteBuffer.allocate(16);
          buffer.putLong(id.getMostSignificantBits());
          buffer.putLong(id.getLeastSignificantBits());
          stream.write(buffer.array());
        }

        return UUID.nameUUIDFromBytes(stream.toByteArray());
      } catch (IOException e) {
        // should not happen as all I/O is in-memory
        throw new RuntimeException(e);
      }
    }
  }
}
