package dev.agst;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "Main", mixinStandardHelpOptions = true, version = "1.0", description = "Implements a basic CLI with server and client subcommands.")
public class Main implements Runnable {

    @Command(name = "server", description = "Starts the server.")
    void server(
            @Option(names = { "-id",
                    "--server-id" }, required = true, description = "The ID for the server.") String serverId) {
        System.out.println("Server started with ID: " + serverId);
        // Server logic here
    }

    @Command(name = "client", description = "Starts the client.")
    void client() {
        System.out.println("Client started.");
        // Client logic here
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        CommandLine.usage(this, System.out);
        System.exit(1);
    }
}