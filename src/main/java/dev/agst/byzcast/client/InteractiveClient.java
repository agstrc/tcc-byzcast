package dev.agst.byzcast.client;

import dev.agst.byzcast.Serializer;
import dev.agst.byzcast.group.GroupProxyRetriever;
import dev.agst.byzcast.message.Request;
import dev.agst.byzcast.message.Response;
import java.util.Scanner;
import java.util.UUID;

public class InteractiveClient {
  Scanner scanner = new Scanner(System.in);

  GroupProxyRetriever proxyRetriever;

  public InteractiveClient(GroupProxyRetriever proxyRetriever) {
    this.proxyRetriever = proxyRetriever;
  }

  public void run() {
    while (true) {
      int fromGroupID = mustParseInt("[fromGroupID]: ");
      var proxy = this.proxyRetriever.forGroup(fromGroupID);

      int targetGroupID = mustParseInt("[targetGroupID]: ");

      System.out.print("[content]: ");
      String content = this.scanner.nextLine();

      var request = new Request(UUID.randomUUID(), new int[]{targetGroupID}, false, content);
      var responseBytes = proxy.invokeOrdered(Serializer.toBytes(request));

      try {
        var response = Serializer.fromBytes(responseBytes, Response.class);
        System.out.println("Response: " + response.toString());
        System.err.println(response.groupIDs().toString() );
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
