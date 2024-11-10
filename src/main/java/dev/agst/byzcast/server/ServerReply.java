package dev.agst.byzcast.server;

import java.io.Serializable;
import java.util.UUID;

/**
 * Represents a reply from the server within the ByzCast protocol.
 *
 * <p>The {@code ServerReply} interface serves as an implementation detail that allows the system to
 * defer the reply of a request. Given the constraints of the bft-smart library, which requires all
 * handling logic for each request batch to be executed before receiving the next batch and only
 * allows replies in raw bytes, the {@code ServerReply} interface enables communication with the
 * {@link ServerReplier} to indicate whether a given message is still being processed.
 *
 * <p>This mechanism is crucial for implementing the logic for receiving requests from other
 * replicas. When ByzCast groups communicate, they send up to N requests to the next group due to
 * the nature of the bft-smart protocol. The handling mechanism ensures that requests are only
 * processed once they have been received N - F times from other groups. The {@code ServerReply}
 * interface, along with the {@link ServerReplier}, facilitates this logic within the bft-smart API.
 */
public sealed interface ServerReply extends Serializable {

  /**
   * Represents a completed reply from the server.
   *
   * <p>This record encapsulates the raw byte data of the reply, indicating that the request has
   * been fully processed.
   */
  public static record Done(byte[] data) implements ServerReply {}

  /**
   * Represents a pending reply from the server.
   *
   * <p>This record encapsulates the unique identifier of the request, indicating that the request
   * is still being processed and a reply will be sent once processing is complete.
   */
  public static record Pending(UUID id) implements ServerReply {}
}
