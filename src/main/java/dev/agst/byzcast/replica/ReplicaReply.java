package dev.agst.byzcast.replica;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * The {@code ReplicaReply} interface plays a pivotal role in the ByzCast system's communication
 * protocol, particularly within the context of the BFT-SMaRT library. It outlines the structure for
 * replies that replicas must generate in response to requests processed by the system. This
 * interface is essential for implementing the N-F processing strategy, where N represents the total
 * number of replicas and F signifies the maximum number of faulty replicas that the system can
 * tolerate. By defining a standard format for replies, {@code ReplicaReply} ensures that the {@code
 * ReplicaReplier} can effectively manage and route responses based on their current state and
 * content.
 *
 * <p>Implementations of this interface are categorized into three distinct records, each serving a
 * unique purpose within the system's communication flow:
 *
 * <ul>
 *   <li>{@code Pending} - Indicates that a request is currently pending and has not yet been fully
 *       processed. This status allows the {@code ReplicaReplier} to queue the request for future
 *       processing, ensuring that it adheres to the N-F processing requirements.
 *   <li>{@code Completed} - Signifies that a request has been processed, containing both the unique
 *       identifier of the request and the result of the processing. This enables the {@code
 *       ReplicaReplier} to forward the processed result to the appropriate client or to aggregate
 *       responses when necessary.
 *   <li>{@code Raw} - Represents a reply containing raw data intended for direct forwarding to the
 *       client. This type bypasses additional processing or aggregation, facilitating immediate
 *       response delivery.
 * </ul>
 *
 * @see dev.agst.byzcast.replica.ReplicaReplier
 */
public sealed interface ReplicaReply extends Serializable {
  record Pending(UUID id) implements ReplicaReply {}

  record Completed(UUID id, byte[] result) implements ReplicaReply {}

  record Raw(byte[] data) implements ReplicaReply {}
}
