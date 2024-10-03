package dev.agst.byzcast.server;

import java.io.Serializable;
import java.util.UUID;

public sealed interface ServerReply extends Serializable {
  public static record Done(byte[] data) implements ServerReply {}

  public static record Pending(UUID id) implements ServerReply {}
}
