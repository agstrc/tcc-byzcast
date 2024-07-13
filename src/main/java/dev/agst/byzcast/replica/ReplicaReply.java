package dev.agst.byzcast.replica;

import java.io.Serializable;
import java.util.UUID;

public sealed interface ReplicaReply extends Serializable {
  record Pending(UUID id) implements ReplicaReply {}

  record Completed(UUID id, byte[] result) implements ReplicaReply {}

  record Raw(byte[] data) implements ReplicaReply {}
}
