package dev.agst.byzcast.message;

import java.io.Serializable;
import java.util.UUID;

public class Request implements Serializable {
  private final UUID id = UUID.randomUUID();

  private final String content;
  private final int targetGroupID;

  public Request(String content, int targetGroupID) {
    this.content = content;
    this.targetGroupID = targetGroupID;
  }

  public UUID getID() {
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
