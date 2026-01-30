package dev.agst.byzcast.topology;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link TopologyManager}. */
class TopologyManagerTest {

  private Topology initialTopology;
  private TopologyManager topologyManager;

  @BeforeEach
  void setUp() throws Exception {
    // Create a simple initial topology: Group 0 -> [Group 1, Group 2]
    List<List<Integer>> topologyList = Arrays.asList(Arrays.asList(1, 2), Arrays.asList(), Arrays.asList());
    var root = TopologyLoader.buildFromList(topologyList);
    initialTopology = new Topology(root);
    topologyManager = new TopologyManager(initialTopology);
  }

  @Test
  void testInitialState() {
    assertEquals(TopologyState.READY, topologyManager.getState());
    assertTrue(topologyManager.isReady());
    assertNotNull(topologyManager.getTopology());
    assertNull(topologyManager.getLastError());
  }

  @Test
  void testConstructorWithNullTopologyThrowsException() {
    assertThrows(IllegalArgumentException.class, () -> new TopologyManager(null));
  }

  @Test
  void testSuccessfulTopologyUpdate() throws Exception {
    // New topology: Group 0 -> [Group 1] -> [Group 2]
    List<List<Integer>> newTopology = Arrays.asList(Arrays.asList(1), Arrays.asList(2), Arrays.asList());

    topologyManager.updateTopology(newTopology);

    assertEquals(TopologyState.READY, topologyManager.getState());
    assertTrue(topologyManager.isReady());
    assertNull(topologyManager.getLastError());

    // Verify the new topology has the expected structure
    var topology = topologyManager.getTopology();
    assertNotNull(topology);
    var groupIds = topology.getGroupIDs();
    assertEquals(3, groupIds.size());
    assertTrue(groupIds.contains(0));
    assertTrue(groupIds.contains(1));
    assertTrue(groupIds.contains(2));
  }

  @Test
  void testTopologyUpdateWithInvalidTopologyFailsWithError() {
    // Invalid topology: Group 0 tries to have ancestor 0 as child (circular dependency)
    List<List<Integer>> invalidTopology = Arrays.asList(Arrays.asList(0));

    assertThrows(TopologyUpdateException.class, () -> topologyManager.updateTopology(invalidTopology));

    assertEquals(TopologyState.ERROR, topologyManager.getState());
    assertFalse(topologyManager.isReady());
    assertNotNull(topologyManager.getLastError());
  }

  @Test
  void testTopologyUpdateWithNullTopologyFailsWithError() {
    assertThrows(TopologyUpdateException.class, () -> topologyManager.updateTopology(null));

    assertEquals(TopologyState.ERROR, topologyManager.getState());
    assertFalse(topologyManager.isReady());
    assertNotNull(topologyManager.getLastError());
  }

  @Test
  void testTopologyUpdateWithEmptyTopologyFailsWithError() {
    assertThrows(TopologyUpdateException.class, () -> topologyManager.updateTopology(new ArrayList<>()));

    assertEquals(TopologyState.ERROR, topologyManager.getState());
    assertFalse(topologyManager.isReady());
    assertNotNull(topologyManager.getLastError());
  }

  @Test
  void testAcknowledgeError() throws Exception {
    // First, cause an error
    List<List<Integer>> invalidTopology = Arrays.asList(Arrays.asList(0));
    assertThrows(TopologyUpdateException.class, () -> topologyManager.updateTopology(invalidTopology));
    assertEquals(TopologyState.ERROR, topologyManager.getState());

    // Acknowledge the error
    assertTrue(topologyManager.acknowledgeError());
    assertEquals(TopologyState.READY, topologyManager.getState());
    assertTrue(topologyManager.isReady());
  }

  @Test
  void testAcknowledgeErrorWhenNotInErrorState() {
    // Manager is in READY state, acknowledgeError should return false
    assertFalse(topologyManager.acknowledgeError());
    assertEquals(TopologyState.READY, topologyManager.getState());
  }

  @Test
  void testOriginalTopologyPreservedAfterFailedUpdate() throws Exception {
    var originalTopology = topologyManager.getTopology();
    var originalGroupIds = originalTopology.getGroupIDs();

    // Attempt invalid update
    List<List<Integer>> invalidTopology = Arrays.asList(Arrays.asList(0));
    assertThrows(TopologyUpdateException.class, () -> topologyManager.updateTopology(invalidTopology));

    // Original topology should still be accessible
    var currentTopology = topologyManager.getTopology();
    assertEquals(originalGroupIds, currentTopology.getGroupIDs());
  }

  @Test
  void testConcurrentReads() throws Exception {
    int numThreads = 10;
    int readsPerThread = 100;
    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(numThreads);
    AtomicInteger successCount = new AtomicInteger(0);

    for (int i = 0; i < numThreads; i++) {
      executor.submit(() -> {
        try {
          startLatch.await();
          for (int j = 0; j < readsPerThread; j++) {
            var topology = topologyManager.getTopology();
            var state = topologyManager.getState();
            if (topology != null && state != null) {
              successCount.incrementAndGet();
            }
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        } finally {
          doneLatch.countDown();
        }
      });
    }

    startLatch.countDown();
    assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
    executor.shutdown();

    assertEquals(numThreads * readsPerThread, successCount.get());
  }

  @Test
  void testMultipleSuccessfulUpdates() throws Exception {
    // First update
    List<List<Integer>> topology1 = Arrays.asList(Arrays.asList(1), Arrays.asList());
    topologyManager.updateTopology(topology1);
    assertEquals(2, topologyManager.getTopology().getGroupIDs().size());

    // Second update
    List<List<Integer>> topology2 = Arrays.asList(Arrays.asList(1, 2), Arrays.asList(), Arrays.asList());
    topologyManager.updateTopology(topology2);
    assertEquals(3, topologyManager.getTopology().getGroupIDs().size());

    // Third update
    List<List<Integer>> topology3 = Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(), Arrays.asList(), Arrays.asList());
    topologyManager.updateTopology(topology3);
    assertEquals(4, topologyManager.getTopology().getGroupIDs().size());

    // State should still be READY
    assertEquals(TopologyState.READY, topologyManager.getState());
  }
}
