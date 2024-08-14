package dev.agst.byzcast;

import bftsmart.tom.ServiceReplica;
import dev.agst.byzcast.Logger.Attr;
import dev.agst.byzcast.client.BatchTestClient;
import dev.agst.byzcast.group.GroupConfigFinder;
import dev.agst.byzcast.group.GroupProxies;
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

    var topology = new Topology(topologyPath);
    var configFinder = new GroupConfigFinder(configsPath);
    var info = new ReplicaInfo(groupID, serverID);
    var logger = new Logger().with(new Attr("GID", groupID), new Attr("SID", serverID));

    var replicaNode =
        ReplicaNode.builder()
            .withLogger(logger)
            .withInfo(info)
            .withConfigFinder(configFinder)
            .withTopology(topology)
            .withTargetRequestCount(3)
            .build();

    new ServiceReplica(
        serverID,
        configFinder.forGroup(groupID),
        replicaNode,
        replicaNode,
        null,
        new ReplicaReplier());

    // some tests showed that we need to keep the main thread alive
    Thread.sleep(Integer.MAX_VALUE);
  }

  @Command(name = "client", description = "Starts the client.")
  void client() throws Exception {
    var topology = new Topology(topologyPath);
    var configFinder = new GroupConfigFinder(configsPath);
    var groupProxies = new GroupProxies(configFinder);

    var client = new BatchTestClient(topology, groupProxies);
    client.run(8);
  }

  public static void main(String[] args) {
    int exitCode = new CommandLine(new Main()).execute(args);
    System.exit(exitCode);
  }
}
