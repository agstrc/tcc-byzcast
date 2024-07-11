package dev.agst.byzcast;

import bftsmart.tom.ServiceReplica;
import bftsmart.tom.server.defaultservices.DefaultReplier;
import dev.agst.byzcast.client.InteractiveClient;
import dev.agst.byzcast.group.GroupConfigFinder;
import dev.agst.byzcast.group.GroupMap;
import dev.agst.byzcast.group.GroupProxyRetriever;
import dev.agst.byzcast.node.Node;
import dev.agst.byzcast.node.RequestHandler;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "byzcast", mixinStandardHelpOptions = true)
public class Main {
  @Option(
      names = {"--configs-home"},
      description = "The path to the directory containing all group configurations",
      required = true)
  String configsPath;

  @Command(name = "server", description = "Starts the server.")
  void server(
      @Option(
              names = {"--server-id"},
              description = "The server ID",
              type = Integer.class,
              required = true)
          Integer serverID,
      @Option(
              names = {"--group-id"},
              description = "The group ID",
              type = Integer.class,
              required = true)
          Integer groupID,
      @Option(
              names = {"--groups-map-file"},
              description =
                  "The file path to the JSON containing the description of group connections",
              required = true)
          String groupsMapFilePath)
      throws Exception {
    var groupMap = new GroupMap(groupsMapFilePath);
    var groupConfigFinder = new GroupConfigFinder(configsPath);
    var groupProxyRetriever = new GroupProxyRetriever(groupConfigFinder);

    var requestHandler = new RequestHandler(groupID, groupMap, groupProxyRetriever);
    var node = new Node(requestHandler);

    new ServiceReplica(
        serverID, groupConfigFinder.forGroup(groupID), node, node, null, new DefaultReplier());

    // TODO: not sure if this is required to keep the server running
    while (true) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Command(name = "client", description = "Starts the client.")
  void client() {
    var configFinder = new GroupConfigFinder(configsPath);
    var proxyRetriever = new GroupProxyRetriever(configFinder);
    var client = new InteractiveClient(proxyRetriever);

    client.run();
  }

  public static void main(String[] args) {
    int exitCode = new CommandLine(new Main()).execute(args);
    System.exit(exitCode);
  }
}
