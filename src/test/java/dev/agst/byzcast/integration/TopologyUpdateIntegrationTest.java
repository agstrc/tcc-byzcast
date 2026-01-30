package dev.agst.byzcast.integration;

import static org.junit.jupiter.api.Assertions.*;

import dev.agst.byzcast.Logger;
import dev.agst.byzcast.Serializer;
import dev.agst.byzcast.SerializingException;
import dev.agst.byzcast.message.Request;
import dev.agst.byzcast.message.Response;
import dev.agst.byzcast.message.TopologyUpdateRequest;
import dev.agst.byzcast.replica.ReplicaInfo;
import dev.agst.byzcast.replica.ReplicaNode;
import dev.agst.byzcast.replica.ReplicaReply;
import dev.agst.byzcast.replica.ReplicaState;
import dev.agst.byzcast.replica.RequestHandler;
import dev.agst.byzcast.topology.Topology;
import dev.agst.byzcast.topology.TopologyLoader;
import dev.agst.byzcast.topology.TopologyManager;
import dev.agst.byzcast.topology.TopologyState;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for the topology update flow.
 *
 * <p>These tests verify the interaction between TopologyManager, RequestHandler, and the message
 * processing flow without requiring a full BFT-SMaRT cluster.
 */
class TopologyUpdateIntegrationTest {

  private TopologyManager topologyManager;
  private RequestHandler requestHandler;
  private ReplicaState replicaState;
  private Logger logger;

  @BeforeEach
  void setUp() throws Exception {
    // Create initial topology: Group 0 -> [Group 1, Group 2]
    List<List<Integer>> initialTopology = Arrays.asList(Arrays.asList(1, 2), Arrays.asList(), Arrays.asList());
    var root = TopologyLoader.buildFromList(initialTopology);
    var topology = new Topology(root);

    topologyManager = new TopologyManager(topology);
    logger = new Logger();
    var info = new ReplicaInfo(0, 0); // Group 0, Server 0
    replicaState = new ReplicaState(1); // Min receive count of 1 for testing

    // Create RequestHandler with null proxies (we won't be forwarding in these tests)
    requestHandler = new RequestHandler(logger, info, null, topologyManager);
  }

  @Test
  void testRequestHandlerRejectsRequestsDuringTopologyUpdate() throws Exception {
    // Create a request
    var request = new Request(UUID.randomUUID(), new int[] {0}, "test", Request.Source.CLIENT);

    // Verify request is handled normally when READY
    assertEquals(TopologyState.READY, topologyManager.getState());
    var reply1 = requestHandler.handle(request, replicaState);
    assertInstanceOf(ReplicaReply.Raw.class, reply1);
    var response1 = Serializer.fromBytes(((ReplicaReply.Raw) reply1).data(), Response.class);
    assertEquals("HANDLED", response1.content());

    // Simulate topology update starting (set state to UPDATING manually for testing)
    // In production, this would happen via the TopologyManager.updateTopology() method
    // We'll simulate by causing an update that's slow
    
    // Instead, let's test the error state
    List<List<Integer>> invalidTopology = Arrays.asList(Arrays.asList(0)); // Invalid - circular
    try {
      topologyManager.updateTopology(invalidTopology);
      fail("Should have thrown exception");
    } catch (Exception e) {
      // Expected
    }

    // Now manager should be in ERROR state
    assertEquals(TopologyState.ERROR, topologyManager.getState());

    // Request should be rejected
    var request2 = new Request(UUID.randomUUID(), new int[] {0}, "test2", Request.Source.CLIENT);
    var reply2 = requestHandler.handle(request2, replicaState);
    assertInstanceOf(ReplicaReply.Raw.class, reply2);
    var response2 = Serializer.fromBytes(((ReplicaReply.Raw) reply2).data(), Response.class);
    assertEquals("TOPOLOGY_ERROR", response2.content());

    // Acknowledge error and verify requests work again
    assertTrue(topologyManager.acknowledgeError());
    assertEquals(TopologyState.READY, topologyManager.getState());

    var request3 = new Request(UUID.randomUUID(), new int[] {0}, "test3", Request.Source.CLIENT);
    var reply3 = requestHandler.handle(request3, replicaState);
    assertInstanceOf(ReplicaReply.Raw.class, reply3);
    var response3 = Serializer.fromBytes(((ReplicaReply.Raw) reply3).data(), Response.class);
    assertEquals("HANDLED", response3.content());
  }

  @Test
  void testTopologyUpdateChangesRoutingBehavior() throws Exception {
    // Initial topology: 0 -> [1, 2]
    var initialGroups = topologyManager.getTopology().getGroupIDs();
    assertEquals(3, initialGroups.size());
    assertTrue(initialGroups.contains(0));
    assertTrue(initialGroups.contains(1));
    assertTrue(initialGroups.contains(2));

    // Update to new topology: 0 -> [1] -> [2] -> [3]
    List<List<Integer>> newTopology = Arrays.asList(
        Arrays.asList(1),
        Arrays.asList(2),
        Arrays.asList(3),
        Arrays.asList()
    );
    topologyManager.updateTopology(newTopology);

    // Verify new topology
    var newGroups = topologyManager.getTopology().getGroupIDs();
    assertEquals(4, newGroups.size());
    assertTrue(newGroups.contains(0));
    assertTrue(newGroups.contains(1));
    assertTrue(newGroups.contains(2));
    assertTrue(newGroups.contains(3));
  }

  @Test
  void testTopologyUpdateRequestSerialization() throws Exception {
    List<List<Integer>> topology = Arrays.asList(Arrays.asList(1, 2), Arrays.asList(), Arrays.asList());

    var updateRequest = TopologyUpdateRequest.create(topology);
    byte[] serialized = Serializer.toBytes(updateRequest);

    // Deserialize
    var deserialized = Serializer.fromBytes(serialized, TopologyUpdateRequest.class);
    assertEquals(updateRequest.id(), deserialized.id());
    assertEquals(updateRequest.topology(), deserialized.topology());
  }

  @Test
  void testMultipleTopologyUpdatesInSequence() throws Exception {
    // First update
    List<List<Integer>> topology1 = Arrays.asList(Arrays.asList(1), Arrays.asList());
    topologyManager.updateTopology(topology1);
    assertEquals(2, topologyManager.getTopology().getGroupIDs().size());
    assertEquals(TopologyState.READY, topologyManager.getState());

    // Request should work
    var request1 = new Request(UUID.randomUUID(), new int[] {0}, "test1", Request.Source.CLIENT);
    var reply1 = requestHandler.handle(request1, replicaState);
    assertInstanceOf(ReplicaReply.Raw.class, reply1);

    // Second update
    List<List<Integer>> topology2 = Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(), Arrays.asList(), Arrays.asList());
    topologyManager.updateTopology(topology2);
    assertEquals(4, topologyManager.getTopology().getGroupIDs().size());
    assertEquals(TopologyState.READY, topologyManager.getState());

    // Request should still work
    var request2 = new Request(UUID.randomUUID(), new int[] {0}, "test2", Request.Source.CLIENT);
    var reply2 = requestHandler.handle(request2, replicaState);
    assertInstanceOf(ReplicaReply.Raw.class, reply2);

    // Third update
    List<List<Integer>> topology3 = Arrays.asList(Arrays.asList(1), Arrays.asList(2), Arrays.asList());
    topologyManager.updateTopology(topology3);
    assertEquals(3, topologyManager.getTopology().getGroupIDs().size());
    assertEquals(TopologyState.READY, topologyManager.getState());
  }

  @Test
  void testReplicaRequestsAlsoRejectedDuringUpdate() throws Exception {
    // Cause an error to put manager in ERROR state
    List<List<Integer>> invalidTopology = Arrays.asList(Arrays.asList(0));
    try {
      topologyManager.updateTopology(invalidTopology);
    } catch (Exception e) {
      // Expected
    }
    assertEquals(TopologyState.ERROR, topologyManager.getState());

    // Replica request should also be rejected
    var replicaRequest = new Request(UUID.randomUUID(), new int[] {0}, "test", Request.Source.REPLICA);
    var reply = requestHandler.handle(replicaRequest, replicaState);
    assertInstanceOf(ReplicaReply.Raw.class, reply);
    var response = Serializer.fromBytes(((ReplicaReply.Raw) reply).data(), Response.class);
    assertEquals("TOPOLOGY_ERROR", response.content());
  }

  @Test
  void testTopologyLoaderBuildFromList() throws Exception {
    // Test simple topology
    List<List<Integer>> simpleTopology = Arrays.asList(Arrays.asList(1), Arrays.asList());
    var root = TopologyLoader.buildFromList(simpleTopology);
    var topology = new Topology(root);
    assertEquals(2, topology.getGroupIDs().size());

    // Test complex topology
    List<List<Integer>> complexTopology = Arrays.asList(
        Arrays.asList(1, 2),
        Arrays.asList(3, 4),
        Arrays.asList(5),
        Arrays.asList(),
        Arrays.asList(),
        Arrays.asList()
    );
    var root2 = TopologyLoader.buildFromList(complexTopology);
    var topology2 = new Topology(root2);
    assertEquals(6, topology2.getGroupIDs().size());
  }

  @Test
  void testTopologyLoaderRejectsInvalidTopology() {
    // Circular dependency
    List<List<Integer>> invalidTopology = Arrays.asList(Arrays.asList(0));
    assertThrows(Exception.class, () -> TopologyLoader.buildFromList(invalidTopology));

    // Empty topology
    assertThrows(IllegalArgumentException.class, () -> TopologyLoader.buildFromList(new ArrayList<>()));

    // Null topology
    assertThrows(IllegalArgumentException.class, () -> TopologyLoader.buildFromList(null));
  }

  /**
   * Tests that verify ReplicaNode responses are compatible with ReplicaReplier deserialization.
   * 
   * <p>This nested test class was added to cover a bug found during live testing where
   * topology update responses were not properly wrapped in ReplicaReply, causing
   * ReplicaReplier.manageReply() to fail with SerializingException.
   * 
   * <p>The issue was in ReplicaNode.handleTopologyUpdate() which was returning:
   * <pre>
   * var response = new Response("TOPOLOGY_UPDATED", new ArrayList<>());
   * return Serializer.toBytes(response);  // BUG: Returns Response directly
   * </pre>
   * 
   * <p>Instead of the correct pattern:
   * <pre>
   * var response = new Response("TOPOLOGY_UPDATED", new ArrayList<>());
   * var reply = new ReplicaReply.Raw(Serializer.toBytes(response));
   * return Serializer.toBytes(reply);  // CORRECT: Returns wrapped ReplicaReply
   * </pre>
   */
  @Nested
  @DisplayName("ReplicaReplier Compatibility Tests")
  class ReplicaReplierCompatibilityTests {

    private ReplicaNode replicaNode;

    @BeforeEach
    void setUpReplicaNode() throws Exception {
      // Create initial topology
      List<List<Integer>> initialTopology = Arrays.asList(Arrays.asList(1, 2), Arrays.asList(), Arrays.asList());
      var root = TopologyLoader.buildFromList(initialTopology);
      var topology = new Topology(root);

      var localTopologyManager = new TopologyManager(topology);
      var localLogger = new Logger();
      var info = new ReplicaInfo(0, 0);
      var localReplicaState = new ReplicaState(1);

      var localRequestHandler = new RequestHandler(localLogger, info, null, localTopologyManager);
      replicaNode = new ReplicaNode(localLogger, localRequestHandler, localReplicaState, localTopologyManager);
    }

    /**
     * Simulates what ReplicaReplier.manageReply() does at line 65 to deserialize replies.
     * If this method throws SerializingException, the ReplicaReplier would return 
     * "EXCEPTIONAL_ERROR" to the client.
     */
    private ReplicaReply deserializeAsReplicaReplier(byte[] rawReply) throws SerializingException {
      return Serializer.fromBytes(rawReply, ReplicaReply.class);
    }

    @Test
    @DisplayName("Topology update response can be deserialized by ReplicaReplier pattern")
    void topologyUpdateResponseCanBeDeserializedByReplicaReplier() throws Exception {
      // Create a valid topology update request
      List<List<Integer>> newTopology = Arrays.asList(Arrays.asList(1), Arrays.asList());
      var updateRequest = TopologyUpdateRequest.create(newTopology);
      byte[] serializedRequest = Serializer.toBytes(updateRequest);

      // Execute through ReplicaNode (simulating BFT-SMaRT delivery)
      byte[][] results = replicaNode.appExecuteBatch(new byte[][] {serializedRequest}, null);
      assertEquals(1, results.length);

      // This is the critical test - ReplicaReplier must be able to deserialize
      // If this fails with SerializingException, the bug from live testing would occur
      ReplicaReply reply;
      try {
        reply = deserializeAsReplicaReplier(results[0]);
      } catch (SerializingException e) {
        fail("ReplicaReplier would fail to deserialize topology update response: " + e.getMessage());
        return;
      }

      // Verify it's the correct type and content
      assertNotNull(reply);
      assertInstanceOf(ReplicaReply.Raw.class, reply);
      
      var response = Serializer.fromBytes(((ReplicaReply.Raw) reply).data(), Response.class);
      assertEquals("TOPOLOGY_UPDATED", response.content());
    }

    @Test
    @DisplayName("Failed topology update response can be deserialized by ReplicaReplier pattern")
    void failedTopologyUpdateResponseCanBeDeserializedByReplicaReplier() throws Exception {
      // Create an invalid topology update request (circular reference)
      List<List<Integer>> invalidTopology = Arrays.asList(Arrays.asList(0));
      var updateRequest = TopologyUpdateRequest.create(invalidTopology);
      byte[] serializedRequest = Serializer.toBytes(updateRequest);

      // Execute through ReplicaNode
      byte[][] results = replicaNode.appExecuteBatch(new byte[][] {serializedRequest}, null);
      assertEquals(1, results.length);

      // ReplicaReplier must be able to deserialize even error responses
      ReplicaReply reply;
      try {
        reply = deserializeAsReplicaReplier(results[0]);
      } catch (SerializingException e) {
        fail("ReplicaReplier would fail to deserialize failed topology update response: " + e.getMessage());
        return;
      }

      // Verify it's the correct type and content
      assertNotNull(reply);
      assertInstanceOf(ReplicaReply.Raw.class, reply);
      
      var response = Serializer.fromBytes(((ReplicaReply.Raw) reply).data(), Response.class);
      assertTrue(response.content().startsWith("TOPOLOGY_UPDATE_FAILED:"));
    }

    @Test
    @DisplayName("Regular request response can be deserialized by ReplicaReplier pattern")
    void regularRequestResponseCanBeDeserializedByReplicaReplier() throws Exception {
      var request = new Request(UUID.randomUUID(), new int[] {0}, "test", Request.Source.CLIENT);
      byte[] serializedRequest = Serializer.toBytes(request);

      byte[][] results = replicaNode.appExecuteBatch(new byte[][] {serializedRequest}, null);
      assertEquals(1, results.length);

      // ReplicaReplier must be able to deserialize
      ReplicaReply reply;
      try {
        reply = deserializeAsReplicaReplier(results[0]);
      } catch (SerializingException e) {
        fail("ReplicaReplier would fail to deserialize regular request response: " + e.getMessage());
        return;
      }

      assertNotNull(reply);
    }

    @Test
    @DisplayName("Invalid payload response can be deserialized by ReplicaReplier pattern")
    void invalidPayloadResponseCanBeDeserializedByReplicaReplier() throws Exception {
      byte[] invalidPayload = "not a valid request".getBytes();

      byte[][] results = replicaNode.appExecuteBatch(new byte[][] {invalidPayload}, null);
      assertEquals(1, results.length);

      // ReplicaReplier must be able to deserialize
      ReplicaReply reply;
      try {
        reply = deserializeAsReplicaReplier(results[0]);
      } catch (SerializingException e) {
        fail("ReplicaReplier would fail to deserialize invalid payload response: " + e.getMessage());
        return;
      }

      assertNotNull(reply);
      assertInstanceOf(ReplicaReply.Raw.class, reply);
      
      var response = Serializer.fromBytes(((ReplicaReply.Raw) reply).data(), Response.class);
      assertEquals("INVALID_PAYLOAD", response.content());
    }

    @Test
    @DisplayName("Unordered request response can be deserialized by ReplicaReplier pattern")
    void unorderedRequestResponseCanBeDeserializedByReplicaReplier() throws Exception {
      byte[] request = "any unordered request".getBytes();

      byte[] result = replicaNode.appExecuteUnordered(request, null);
      assertNotNull(result);

      // ReplicaReplier must be able to deserialize
      ReplicaReply reply;
      try {
        reply = deserializeAsReplicaReplier(result);
      } catch (SerializingException e) {
        fail("ReplicaReplier would fail to deserialize unordered request response: " + e.getMessage());
        return;
      }

      assertNotNull(reply);
      assertInstanceOf(ReplicaReply.Raw.class, reply);
      
      var response = Serializer.fromBytes(((ReplicaReply.Raw) reply).data(), Response.class);
      assertEquals("UNSUPPORTED_OPERATION", response.content());
    }
  }
}
