package dev.agst.byzcast;

import bftsmart.tom.ServiceReplica;
import dev.agst.byzcast.client.InteractiveClient;
import dev.agst.byzcast.replica.ReplicaInfo;
import dev.agst.byzcast.replica.ReplicaNode;
import dev.agst.byzcast.replica.ReplicaReplier;
import dev.agst.byzcast.topology.Topology;
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

  @Option(
      names = {"--groups-map-file"},
      description = "The file path to the JSON containing the description of group connections",
      required = true)
  String groupsMapFilePath;

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
          Integer groupID)
      throws Exception {

    var topology = new Topology(groupsMapFilePath, configsPath);
    var replicaNode = new ReplicaNode(3, new ReplicaInfo(groupID, serverID), topology);

    new ServiceReplica(
        serverID,
        topology.configPathForGroup(groupID),
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
  void client() throws Exception {
    var topology = new Topology(groupsMapFilePath, configsPath);
    var client = new InteractiveClient(topology);

    client.run();
  }

  public static void main(String[] args) {
    int exitCode = new CommandLine(new Main()).execute(args);
    System.exit(exitCode);
  }
}
