package dev.agst.byzcast.client;

import dev.agst.byzcast.Logger;
import dev.agst.byzcast.group.GroupConfigFinder;
import dev.agst.byzcast.group.GroupProxies;
import dev.agst.byzcast.topology.Topology;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * Manages the execution of multiple client threads in the ByzCast system.
 *
 * <p>This class is responsible for creating and running multiple {@link ClientThread} instances,
 * collecting their statistics, and writing the statistics to files.
 */
public class BatchClients {
  private final Logger logger;
  private final Topology topology;
  private final GroupConfigFinder configFinder;

  private int clientCount = 8;
  private int runtimeMillis = 120_000;

  private String statsDir = ".";

  private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

  /**
   * Constructs a new {@code BatchClients} instance with the specified parameters.
   *
   * @param logger The logger to use for logging messages.
   * @param topology The topology of the ByzCast system.
   * @param configFinder The configuration finder for group proxies.
   */
  public BatchClients(Logger logger, Topology topology, GroupConfigFinder configFinder) {
    this.logger = logger;
    this.topology = topology;
    this.configFinder = configFinder;
  }

  /**
   * Sets the number of client threads to run.
   *
   * @param clientCount The number of client threads.
   * @return The current {@code BatchClients} instance.
   */
  public BatchClients withClientCount(int clientCount) {
    this.clientCount = clientCount;
    return this;
  }

  /**
   * Sets the runtime duration for the client threads.
   *
   * @param runtimeMillis The runtime duration in milliseconds.
   * @return The current {@code BatchClients} instance.
   */
  public BatchClients withRuntimeMillis(int runtimeMillis) {
    this.runtimeMillis = runtimeMillis;
    return this;
  }

  /**
   * Sets the directory for storing statistics files.
   *
   * @param statsDir The directory for storing statistics files.
   * @return The current {@code BatchClients} instance.
   */
  public BatchClients withStatsDir(String statsDir) {
    this.statsDir = statsDir;
    return this;
  }

  /** Runs the client threads, collects their statistics, and writes the statistics to files. */
  public void run() {
    var random = new Random();

    var futures = new ArrayList<Future<List<Stat>>>();

    for (int i = 0; i < clientCount; i++) {
      var proxies = new GroupProxies(configFinder);
      var clientLogger = logger.with(new Logger.Attr("clientID", i));

      var client = new ClientThread(clientLogger, topology, proxies);
      client.setRuntimeMillis(runtimeMillis);

      futures.add(executor.submit(client::run));

      // the sleep logic between client starts is copied from the original byzcast
      // implementation.
      try {
        Thread.sleep(random.nextInt(600));
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    // collect all stats before writing them
    var clientsStats =
        futures.stream()
            .map(
                future -> {
                  try {
                    return future.get();
                  } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                  }
                })
            .collect(Collectors.toList());
    logger.info("All clients done");

    try {
      Files.createDirectories(Path.of(statsDir));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    for (int i = 0; i < clientsStats.size(); i++) {
      var stats = clientsStats.get(i);
      var clientNum = String.format("%04d", i);
      var fileName = String.format("stats-%s.txt", clientNum);
      var path = Path.of(statsDir, fileName);

      try (var file =
          Files.newBufferedWriter(
              path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
        StatsWriter.writeStats(file, stats);
        logger.info("Wrote stats file", new Logger.Attr("path", path));
      } catch (IOException e) {
        logger.error("Failed to write stats", e);
      }
    }
  }
}
