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
      names = {"--groups-configs"},
      description = "Path for diretory containing all group configurations",
      required = true)
  String configsPath;

  @Option(
      names = {"--topology"},
      description = "Path for the JSON file containing the description of group connections",
      required = true)
  String topologyPath;

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

    var topology = new Topology(topologyPath, configsPath);
    var replicaNode = new ReplicaNode(3, new ReplicaInfo(groupID, serverID), topology);

    new ServiceReplica(
        serverID,
        topology.configPathForGroup(groupID),
        replicaNode,
        replicaNode,
        null,
        new ReplicaReplier());

    // some tests showed that we need to keep the main thread alive
    Thread.sleep(Integer.MAX_VALUE);
  }

  @Command(name = "client", description = "Starts the client.")
  void client() throws Exception {
    var topology = new Topology(topologyPath, configsPath);
    var client = new InteractiveClient(topology);

    client.run();
  }

  public static void main(String[] args) {
    int exitCode = new CommandLine(new Main()).execute(args);
    System.exit(exitCode);
  }
}
