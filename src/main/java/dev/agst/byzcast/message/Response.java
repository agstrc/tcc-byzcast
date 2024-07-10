package dev.agst.byzcast.message;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.Serializable;
import java.util.ArrayList;

public class Response implements Serializable {
  private final String content;

  private final ArrayList<Integer> groupIDs = new ArrayList<>();

  public Response(String content, int currentGroupID) {
    this.content = content;
    this.groupIDs.add(currentGroupID);
  }

  public String getContent() {
    return content;
  }

  public ArrayList<Integer> getGroupIDs() {
    return groupIDs;
  }

  public static Response fromBytes(byte[] bytes) throws MessageDeserializationException {
    return Serializer.responseFromBytes(bytes);
  }

  public byte[] toBytes() {
    return Serializer.toBytes(this);
  }

  public void addTargetGroupID(int groupID) {
    groupIDs.add(groupID);
  }

  @Override
  public String toString() {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    return gson.toJson(this);
  }
}
