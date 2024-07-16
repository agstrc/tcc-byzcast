package dev.agst.byzcast.client;

import com.google.gson.FormattingStyle;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.agst.byzcast.Serializer;
import dev.agst.byzcast.group.GroupProxies;
import dev.agst.byzcast.message.Request;
import dev.agst.byzcast.message.Response;
import dev.agst.byzcast.topology.Topology;
import java.util.Arrays;
import java.util.Scanner;
import java.util.UUID;

public class InteractiveClient {
  Scanner scanner = new Scanner(System.in);
  Gson gson =
      new GsonBuilder().setPrettyPrinting().setFormattingStyle(FormattingStyle.PRETTY).create();

  // GroupProxyRetriever proxyRetriever;
  Topology topology;
  GroupProxies groupProxies;

  public InteractiveClient(Topology topology, GroupProxies groupProxies) {
    this.topology = topology;
    this.groupProxies = groupProxies;
  }

  public void run() {
    while (true) {
      int fromGroupID = mustParseInt("[fromGroupID]: ");
      var proxy = groupProxies.forGroup(fromGroupID);

      System.out.print("[targetGroupIDs, comma-separated]: ");
      String targetGroupIDsInput = this.scanner.nextLine();
      int[] targetGroupIDs =
          Arrays.stream(targetGroupIDsInput.split(","))
              .map(String::trim)
              .mapToInt(Integer::parseInt)
              .toArray();

      System.out.print("[content]: ");
      String content = this.scanner.nextLine();

      var request = new Request(UUID.randomUUID(), targetGroupIDs, content, Request.Source.CLIENT);
      var responseBytes = proxy.invokeOrdered(Serializer.toBytes(request));

      try {
        var response = Serializer.fromBytes(responseBytes, Response.class);
        System.out.println("Response: " + gson.toJson(response));
      } catch (Exception e) {
        System.out.println("Error: " + e.getMessage());
      }
    }
  }

  private int mustParseInt(String prompt) {
    while (true) {
      System.out.print(prompt);
      try {
        return Integer.parseInt(this.scanner.nextLine());
      } catch (NumberFormatException e) {
        System.out.println("Invalid number");
      }
    }
  }
}
