package dev.agst.byzcast;

import bftsmart.tom.ServiceReplica;
import bftsmart.tom.server.defaultservices.DefaultReplier;
import dev.agst.byzcast.exceptions.InvalidConfigException;
import dev.agst.byzcast.groups.GroupConfigHomeFinder;
import dev.agst.byzcast.groups.GroupProxyRetriever;
import dev.agst.byzcast.groups.GroupsConfigLoader;

import java.io.IOException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "Main", mixinStandardHelpOptions = true)
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
      throws IOException, InvalidConfigException {

    var configHomeFinder = new GroupConfigHomeFinder(configsPath);

    var loader = new GroupsConfigLoader();
    var groupsConfig = loader.loadFromJson(groupsMapFilePath);
    var messageServer = new MessageServer(groupID, groupsConfig, configHomeFinder);

    new ServiceReplica(
        serverID,
        configHomeFinder.forGroup(groupID),
        messageServer,
        messageServer,
        null,
        new DefaultReplier());

    // TODO: validar a necessidade disso para manter o servidor rodando
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
    var retriever = new GroupProxyRetriever(new GroupConfigHomeFinder(configsPath));
    var client = new InteractiveMessageClient(retriever);

    client.sendLoop();
  }

  public static void main(String[] args) {
    int exitCode = new picocli.CommandLine(new Main()).execute(args);
    System.exit(exitCode);
  }
}
