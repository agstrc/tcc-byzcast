package dev.agst.byzcast;

import dev.agst.byzcast.groups.GroupProxyRetriever;
import java.util.Scanner;

/** The MessageClient class represents a client that sends messages to a group. */
public class InteractiveMessageClient {
  private GroupProxyRetriever groupProxyRetriever;

  /**
   * Constructs a new MessageClient with the specified GroupProxyRetriever.
   *
   * @param groupProxyRetriever the GroupProxyRetriever used to retrieve group proxies
   */
  InteractiveMessageClient(GroupProxyRetriever groupProxyRetriever) {
    this.groupProxyRetriever = groupProxyRetriever;
  }

  /**
   * Sends messages to a group in an interactive loop. This method prompts the user for a group ID
   * and a message, and sends the message to the corresponding group. The loop continues
   * indefinitely until the program is terminated.
   */
  public void sendLoop() {
    while (true) {
      var scanner = new Scanner(System.in);

      int fromGroupID; // the group which the message will be sent from
      int toGroupID; // the target group of the message

      fromGroupID = mustScanInt(scanner, "[fromGroup]: ");
      toGroupID = mustScanInt(scanner, "[toGroup]: ");

      System.out.print("[message] > ");
      var line = scanner.nextLine();

      var message = new Message(line, toGroupID);
      var proxy = groupProxyRetriever.getProxyForGroup(fromGroupID);
      var response = proxy.invokeOrdered(message.toBytes());
      System.out.println("[response]: " + new String(response));
    }
  }

  int mustScanInt(Scanner scanner, String prompt) {
    while (true) {
      try {
        System.out.print(prompt);
        var scanned = scanner.nextLine();

        return Integer.parseInt(scanned);
      } catch (NumberFormatException e) {
        System.out.println("Invalid input. Please enter a number.");
      }
    }
  }

  void interactiveSend() {
    // try (var scanner = new Scanner(System.in)) {
    //   int groupID;
    //   while (true) {
    //     System.out.print("[groupID] > ");
    //     try {
    //       groupID = scanner.nextInt();
    //       break;
    //     } catch (InputMismatchException e) {
    //       System.out.println("Invalid input. Please enter a number.");
    //     }
    //   }

    //   System.out.print("[message] > ");
    //   var message = scanner.next();

    //   Message messageObj = new Message(message, groupID);

    //   var serviceProxy = groupProxyRetriever.getProxyForGroup(groupID);
    //   var response = serviceProxy.invokeOrdered(messageObj.toBytes());
    //   System.out.println("[reponse]: " + new String(response));
    // }
  }
}
