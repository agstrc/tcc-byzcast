package dev.agst.byzcast;

import bftsmart.tom.ServiceReplica;
import dev.agst.byzcast.Logger.Attr;
import dev.agst.byzcast.client.BatchClients;
import dev.agst.byzcast.group.GroupConfigFinder;
import dev.agst.byzcast.group.GroupProxies;
import dev.agst.byzcast.server.RequestHandler;
import dev.agst.byzcast.server.ServerNode;
import dev.agst.byzcast.server.ServerReplier;
import dev.agst.byzcast.server.ServerState;
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

  @Option(
      names = {"--log"},
      description = "Whether to log messages or not",
      defaultValue = "true",
      type = Boolean.class)
  boolean log;

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
    var groupProxies = new GroupProxies(configFinder);
    var logger = new Logger().with(new Attr("GID", groupID), new Attr("SID", serverID));

    if (!log) {
      logger.disable();
    }

    var replier = new ServerReplier();
    var handler = new RequestHandler(logger, groupID, groupProxies, topology, replier);
    var state = new ServerState(3);
    var serverNode = new ServerNode(handler, state);

    new ServiceReplica(
        serverID, configFinder.forGroup(groupID), serverNode, serverNode, null, replier);

    // some tests showed that we need to keep the main thread alive
    Thread.sleep(Integer.MAX_VALUE);
  }

  @Command(name = "client", description = "Starts the client.")
  void client() throws Exception {
    var topology = new Topology(topologyPath);
    var configFinder = new GroupConfigFinder(configsPath);

    var batch = new BatchClients(new Logger(), topology, configFinder);
    batch.withClientCount(1).run();
  }

  public static void main(String[] args) {
    int exitCode = new CommandLine(new Main()).execute(args);
    System.exit(exitCode);
  }
}
