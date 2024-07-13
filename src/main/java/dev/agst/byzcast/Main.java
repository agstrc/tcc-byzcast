package dev.agst.byzcast;

import bftsmart.tom.ServiceReplica;
import dev.agst.byzcast.client.InteractiveClient;
import dev.agst.byzcast.group.GroupConfigFinder;
import dev.agst.byzcast.group.GroupMap;
import dev.agst.byzcast.group.GroupProxyRetriever;
import dev.agst.byzcast.replica.ReplicaInfo;
import dev.agst.byzcast.replica.ReplicaNode;
import dev.agst.byzcast.replica.ReplicaReplier;
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

    var replicaNode =
        new ReplicaNode(3, new ReplicaInfo(groupID, serverID), groupMap, groupProxyRetriever);

    new ServiceReplica(
        serverID,
        groupConfigFinder.forGroup(groupID),
        replicaNode,
        replicaNode,
        null,
        new ReplicaReplier());

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
