package dev.agst.byzcast.client;

import dev.agst.byzcast.Serializer;
import dev.agst.byzcast.message.Request;
import dev.agst.byzcast.message.Response;
import dev.agst.byzcast.topology.Topology;
import java.util.Scanner;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class InteractiveClient {
  Scanner scanner = new Scanner(System.in);
  Gson gson = new GsonBuilder().setPrettyPrinting().create();

  // GroupProxyRetriever proxyRetriever;
  Topology topology;

  public InteractiveClient(Topology topology) {
    this.topology = topology;
  }

  public void run() {
    while (true) {
      int fromGroupID = mustParseInt("[fromGroupID]: ");
      var proxy = this.topology.getServiceProxy(fromGroupID);

      int targetGroupID = mustParseInt("[targetGroupID]: ");

      System.out.print("[content]: ");
      String content = this.scanner.nextLine();

      var request = new Request(UUID.randomUUID(), new int[] {targetGroupID}, false, content);
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
