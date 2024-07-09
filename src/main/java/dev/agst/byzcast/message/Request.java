package dev.agst.byzcast.message;

import java.util.UUID;

public class Request {
  private final UUID id = UUID.randomUUID();

  private final String content;
  private final int targetGroupID;

  Request(String content, int targetGroupID) {
    this.content = content;
    this.targetGroupID = targetGroupID;
  }

  public UUID getId() {
    return id;
  }

  public String getContent() {
    return content;
  }

  public int getTargetGroupID() {
    return targetGroupID;
  }

  public static Request fromBytes(byte[] bytes) throws MessageDeserializationException {
    return Serializer.requestFromBytes(bytes);
  }

  public byte[] toBytes() {
    return Serializer.toBytes(this);
  }
}
