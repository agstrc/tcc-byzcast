package dev.agst.byzcast;

import java.io.IOException;
import java.util.Scanner;

import bftsmart.tom.ServiceReplica;
import bftsmart.tom.server.defaultservices.DefaultReplier;
import dev.agst.byzcast.exceptions.InvalidConfigException;
import dev.agst.byzcast.groups.GroupsConfigLoader;
import dev.agst.byzcast.utils.ConfigHomeFinder;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "Main", mixinStandardHelpOptions = true)
public class Main {

    @Command(name = "server", description = "Starts the server.")
    void server(
            @Parameters(description = "The server ID", type = Integer.class) Integer serverID,
            @Parameters(description = "The group ID", type = Integer.class) Integer groupID)
            throws IOException, InvalidConfigException {

        var configHomeFinder = new ConfigHomeFinder("/workspaces/byzcast-tcc/config");

        var loader = new GroupsConfigLoader();
        var groupsConfig = loader.loadFromJson("/workspaces/byzcast-tcc/src/main/resources/cfg.json");
        var messageServer = new MessageServer(groupID, groupsConfig, configHomeFinder);

        new ServiceReplica(serverID, configHomeFinder.forGroup(groupID), messageServer, messageServer, null,
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
