package dev.agst.byzcast;

import bftsmart.tom.ServiceReplica;
import bftsmart.tom.server.defaultservices.DefaultReplier;
import dev.agst.byzcast.exceptions.InvalidConfigException;
import dev.agst.byzcast.groups.GroupsConfigLoader;
import dev.agst.byzcast.utils.ConfigHomeFinder;
import java.io.IOException;
import java.util.Scanner;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "Main", mixinStandardHelpOptions = true)
public class Main {

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
      throws IOException, InvalidConfigException {

    var configHomeFinder = new ConfigHomeFinder("byzcast/configs");

    var loader = new GroupsConfigLoader();
    var groupsConfig = loader.loadFromJson("byzcast/config.json");
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
    var messageClient = new MessageClient();
    System.out.print("$: ");

    var scanner = new Scanner(System.in);
    String input = scanner.nextLine();

    var data = input.split("\s");

    messageClient.send(new Message(data[1], Integer.parseInt(data[0])));
    scanner.close();
  }

  public static void main(String[] args) {
    int exitCode = new picocli.CommandLine(new Main()).execute(args);
    System.exit(exitCode);
  }
}
