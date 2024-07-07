package dev.agst.byzcast;

import java.util.Scanner;

import bftsmart.tom.ServiceReplica;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "Main", mixinStandardHelpOptions = true)
public class Main {

    @Command(name = "server", description = "Starts the server.")
    void server(
            @Parameters(description = "The server ID", type = Integer.class) Integer serverId) {
        var messageServer = new MessageServer();
        new ServiceReplica(serverId, messageServer, messageServer);

        // TODO: this might not be needed, but previous testes proved the replica to
        // stop without it
        while (true) {
            try {
                Thread.sleep(10000);
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

        messageClient.send(new Message(input));
        scanner.close();
    }

    public static void main(String[] args) {
        int exitCode = new picocli.CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

}
