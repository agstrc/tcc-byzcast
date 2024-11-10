package dev.agst.byzcast.client;

import dev.agst.byzcast.Logger;
import dev.agst.byzcast.Logger.Attr;
import dev.agst.byzcast.Serializer;
import dev.agst.byzcast.SerializingException;
import dev.agst.byzcast.group.GroupProxies;
import dev.agst.byzcast.message.Request.ClientRequest;
import dev.agst.byzcast.message.Response;
import dev.agst.byzcast.topology.Topology;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Represents a client thread that sends requests to the ByzCast system.
 *
 * <p>This class is responsible for generating requests, sending them to the appropriate groups, and
 * collecting statistics about the requests and responses.
 */
class ClientThread {
  private final Logger logger;
  private final Topology topology;
  private final GroupProxies proxies;

  /**
   * Constructs a new {@code ClientThread} with the specified parameters.
   *
   * @param logger The logger to use for logging messages.
   * @param topology The topology of the ByzCast system.
   * @param proxies The group proxies for sending requests.
   */
  public ClientThread(Logger logger, Topology topology, GroupProxies proxies) {
    this.logger = logger;
    this.topology = topology;
    this.proxies = proxies;
  }

  /**
   * Runs the client thread, sending requests and collecting statistics.
   *
   * @param stopFlag An {@link AtomicBoolean} flag used to signal the thread to stop.
   * @return A list of {@link Stat} objects representing the statistics of the requests and
   *     responses.
   */
  public List<Stat> run(AtomicBoolean stopFlag) {
    logger.info("Client started");

    var targetGroups = selectTargets();
    var optionLCA = topology.findLCA(targetGroups);
    if (optionLCA.isEmpty()) {
      logger.error("No LCA found for groups: " + targetGroups);
      throw new RuntimeException("No LCA found for groups: " + targetGroups);
    }

    var lca = optionLCA.get();
    var proxy = proxies.forGroup(lca);

    var stats = new ArrayList<Stat>();

    while (!stopFlag.get()) {
      var request = new ClientRequest(UUID.randomUUID(), new ArrayList<>(targetGroups), "req");

      var beforeRequest = currenTimeMicros();
      var response = proxy.invokeOrdered(Serializer.toBytes(request));
      var afterRequest = currenTimeMicros();

      if (response == null) {
        // usually means a timeout, but the bft-smart lib usually logs the actual issue
        logger.error("Request failed", new Attr("RID", request.id()));
        continue;
      }

      stats.add(new Stat(request.id(), beforeRequest, afterRequest, lca, targetGroups));

      try {
        Serializer.fromBytes(response, Response.class);
      } catch (SerializingException e) {
        logger.error("Failed to deserialize response", e);
      }
    }

    logger.info("Client finished");
    return stats;
  }

  /**
   * Selects target groups for sending requests.
   *
   * <p>This method currently uses a simple random selection logic, but it can be modified to
   * implement different selection strategies.
   *
   * @return A list of target group IDs.
   */
  private List<Integer> selectTargets() {
    // currently, this method is manually changed whenever an experiment requires a different
    // selection logic (such as skewed selection). A dynamic selection logic can be implemented
    // here.
    var totalTargets = 2;
    var possibleTargets = new ArrayList<>(topology.getGroupIDs());

    Collections.shuffle(possibleTargets);
    return possibleTargets.subList(0, totalTargets);
  }

  /**
   * Returns the current time in microseconds.
   *
   * @return The current time in microseconds.
   */
  private static long currenTimeMicros() {
    return System.nanoTime() / 1_000;
  }
}
