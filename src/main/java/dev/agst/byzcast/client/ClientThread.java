package dev.agst.byzcast.client;

import dev.agst.byzcast.Logger;
import dev.agst.byzcast.Serializer;
import dev.agst.byzcast.group.GroupProxies;
import dev.agst.byzcast.message.Request.ClientRequest;
import dev.agst.byzcast.topology.Topology;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

class ClientThread {
  private final Logger logger;
  private final Topology topology;
  private final GroupProxies proxies;

  private int runtimeMillis = 120_000;

  public void setRuntimeMillis(int runtimeMillis) {
    this.runtimeMillis = runtimeMillis;
  }

  public ClientThread(Logger logger, Topology topology, GroupProxies proxies) {
    this.logger = logger;
    this.topology = topology;
    this.proxies = proxies;
  }

  public List<Stat> run() {
    var startTime = System.currentTimeMillis();
    var stats = new ArrayList<Stat>();

    logger.info("Client started");
    while (System.currentTimeMillis() - startTime < runtimeMillis) {
      var targetGroups = selectTargets();
      var optionLCA = topology.findLCA(targetGroups);
      if (optionLCA.isEmpty()) {
        logger.error("No LCA found for groups: " + targetGroups);
        continue;
      }

      var lca = optionLCA.get();
      var proxy = proxies.forGroup(lca);

      var targetGroupsArray = targetGroups.stream().mapToInt(Integer::intValue).toArray();
      var request = new ClientRequest(UUID.randomUUID(), new ArrayList<>(targetGroups), "req");

      // new ClientRequest();
      // var request = new Request(UUID.randomUUID(), targetGroupsArray, "req",
      // Request.Source.CLIENT);

      var beforeRequest = currenTimeMicros();
      var response = proxy.invokeOrdered(Serializer.toBytes(request));
      var afterRequest = currenTimeMicros();

      stats.add(new Stat(request.id(), beforeRequest, afterRequest, lca, targetGroups));
      // try {
      //   Serializer.fromBytes(response, Response.class);
      // } catch (Exception e) {
      //   var errorAttr = new Logger.Attr("responseBytes", response);
      //   logger.error("Failed to deserialize response: %s", errorAttr);
      //   continue;
      // }
    }

    logger.info("Client finished");
    return stats;
  }

  private List<Integer> selectTargets() {
    // currently, this method is manually changed whenever an experiment requires a different
    // selection logic (such as skewed selection). A dynamic selection logic can be implemented
    // here.
    var totalTargets = 2;
    var possibleTargets = new ArrayList<>(topology.getGroupIDs());

    Collections.shuffle(possibleTargets);
    return possibleTargets.subList(0, totalTargets);
  }

  private static long currenTimeMicros() {
    return System.nanoTime() / 1_000;
  }
}
