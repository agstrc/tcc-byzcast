package dev.agst.byzcast.client;

import com.google.gson.FormattingStyle;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.agst.byzcast.Logger;
import dev.agst.byzcast.Logger.Attr;
import dev.agst.byzcast.Serializer;
import dev.agst.byzcast.group.GroupProxies;
import dev.agst.byzcast.message.Request;
import dev.agst.byzcast.message.Response;
import dev.agst.byzcast.topology.Topology;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class BatchTestClient {
  private final Gson gson =
      new GsonBuilder().setPrettyPrinting().setFormattingStyle(FormattingStyle.PRETTY).create();

  private final Topology topology;
  private final GroupProxies proxies;
  private final AtomicBoolean shutdownRequested = new AtomicBoolean(false);

  public BatchTestClient(Topology topology, GroupProxies proxies) {
    this.topology = topology;
    this.proxies = proxies;

    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  shutdownRequested.set(true);
                  System.out.println("Shutdown signal received.");
                }));
  }

  public void run(int numThreads) throws InterruptedException {
    var executor = Executors.newVirtualThreadPerTaskExecutor();

    for (int i = 0; i < numThreads; i++) {
      executor.submit(this::sendLoop);
    }

    executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
  }

  private void sendLoop() {
    var logger = new Logger();
    var threadID = Thread.currentThread().threadId();

    logger = logger.with(new Attr("TID", threadID));

    while (!shutdownRequested.get()) {
      try {
        var targetGroupIDs = selectRandomGroups(topology.getGroupIDs());
        var optLca = topology.findLCA(targetGroupIDs);
        if (optLca.isEmpty()) {
          logger.error("No common ancestor found");
          continue;
        }

        var lca = optLca.get();
        logger.info("LCA found", new Attr("LCA", lca));

        var proxy = proxies.forGroup(lca);

        var targetGroupIDsArray = targetGroupIDs.stream().mapToInt(Integer::intValue).toArray();
        var request =
            new Request(
                UUID.randomUUID(), targetGroupIDsArray, "some-content", Request.Source.CLIENT);
        var responseBytes = proxy.invokeOrdered(Serializer.toBytes(request));

        var response = Serializer.fromBytes(responseBytes, Response.class);
        logger.info(gson.toJson(response));
      } catch (Exception e) {
        logger.error("Error", e);
      }
    }
  }

  private static List<Integer> selectRandomGroups(Set<Integer> groupIDs) {
    var list = new ArrayList<>(groupIDs);
    Collections.shuffle(list);
    var numGroups = new Random().nextInt(1, list.size() + 1);
    return list.subList(0, numGroups);
  }
}
