package dev.agst.byzcast.replica;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import dev.agst.byzcast.Logger;
import dev.agst.byzcast.Serializer;
import dev.agst.byzcast.SerializingException;
import dev.agst.byzcast.message.Request;
import dev.agst.byzcast.message.Response;
import dev.agst.byzcast.message.TopologyUpdateRequest;
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
 * Tests for ReplicaNode, specifically focusing on the serialization/deserialization
 * compatibility with ReplicaReplier.
 *
 * <p>These tests verify that all responses from ReplicaNode can be properly deserialized
 * as ReplicaReply objects, which is required by ReplicaReplier.manageReply().
 */
class ReplicaNodeTest {

  private ReplicaNode replicaNode;
  private TopologyManager topologyManager;
  private RequestHandler requestHandler;
  private ReplicaState replicaState;
  private Logger logger;

  @BeforeEach
  void setUp() throws Exception {
    // Create initial topology
    List<List<Integer>> initialTopology = Arrays.asList(Arrays.asList(1, 2), Arrays.asList(), Arrays.asList());
    var root = TopologyLoader.buildFromList(initialTopology);
    var topology = new Topology(root);

    topologyManager = new TopologyManager(topology);
    logger = new Logger();
    var info = new ReplicaInfo(0, 0);
    replicaState = new ReplicaState(1);

    requestHandler = new RequestHandler(logger, info, null, topologyManager);
    replicaNode = new ReplicaNode(logger, requestHandler, replicaState, topologyManager);
  }

  /**
   * Helper method to simulate what ReplicaReplier does when deserializing replies.
   * This is the exact pattern used in ReplicaReplier.manageReply() at line 65.
   */
  private ReplicaReply deserializeAsReplicaReplier(byte[] rawReply) throws SerializingException {
    return Serializer.fromBytes(rawReply, ReplicaReply.class);
  }

  /**
   * Helper method to extract Response from a ReplicaReply.Raw.
   */
  private Response extractResponse(ReplicaReply reply) throws SerializingException {
    assertInstanceOf(ReplicaReply.Raw.class, reply, "Reply should be Raw type");
    return Serializer.fromBytes(((ReplicaReply.Raw) reply).data(), Response.class);
  }

  @Nested
  @DisplayName("Topology Update Response Serialization")
  class TopologyUpdateResponseSerialization {

    @Test
    @DisplayName("Successful topology update returns ReplicaReply that can be deserialized by ReplicaReplier")
    void successfulTopologyUpdateReturnsDeserializableReplicaReply() throws Exception {
      // Create a valid topology update request
      List<List<Integer>> newTopology = Arrays.asList(Arrays.asList(1), Arrays.asList());
      var updateRequest = TopologyUpdateRequest.create(newTopology);
      byte[] serializedRequest = Serializer.toBytes(updateRequest);

      // Execute the request through ReplicaNode
      byte[][] results = replicaNode.appExecuteBatch(new byte[][] {serializedRequest}, null);
      assertEquals(1, results.length);

      // Verify the response can be deserialized as ReplicaReply (like ReplicaReplier does)
      ReplicaReply reply = deserializeAsReplicaReplier(results[0]);
      assertNotNull(reply, "Reply should not be null");
      assertInstanceOf(ReplicaReply.Raw.class, reply, "Reply should be Raw type");

      // Verify the response content
      Response response = extractResponse(reply);
      assertEquals("TOPOLOGY_UPDATED", response.content());
    }

    @Test
    @DisplayName("Failed topology update returns ReplicaReply that can be deserialized by ReplicaReplier")
    void failedTopologyUpdateReturnsDeserializableReplicaReply() throws Exception {
      // Create an invalid topology update request (circular dependency)
      List<List<Integer>> invalidTopology = Arrays.asList(Arrays.asList(0));
      var updateRequest = TopologyUpdateRequest.create(invalidTopology);
      byte[] serializedRequest = Serializer.toBytes(updateRequest);

      // Execute the request through ReplicaNode
      byte[][] results = replicaNode.appExecuteBatch(new byte[][] {serializedRequest}, null);
      assertEquals(1, results.length);

      // Verify the response can be deserialized as ReplicaReply (like ReplicaReplier does)
      ReplicaReply reply = deserializeAsReplicaReplier(results[0]);
      assertNotNull(reply, "Reply should not be null");
      assertInstanceOf(ReplicaReply.Raw.class, reply, "Reply should be Raw type");

      // Verify the response content indicates failure
      Response response = extractResponse(reply);
      assertTrue(response.content().startsWith("TOPOLOGY_UPDATE_FAILED:"));
    }

    @Test
    @DisplayName("Multiple consecutive topology updates all return deserializable ReplicaReplies")
    void multipleTopologyUpdatesReturnDeserializableReplies() throws Exception {
      // First update
      List<List<Integer>> topology1 = Arrays.asList(Arrays.asList(1), Arrays.asList());
      var request1 = TopologyUpdateRequest.create(topology1);
      byte[][] results1 = replicaNode.appExecuteBatch(new byte[][] {Serializer.toBytes(request1)}, null);
      ReplicaReply reply1 = deserializeAsReplicaReplier(results1[0]);
      assertNotNull(reply1);
      assertEquals("TOPOLOGY_UPDATED", extractResponse(reply1).content());

      // Second update
      List<List<Integer>> topology2 = Arrays.asList(Arrays.asList(1, 2), Arrays.asList(), Arrays.asList());
      var request2 = TopologyUpdateRequest.create(topology2);
      byte[][] results2 = replicaNode.appExecuteBatch(new byte[][] {Serializer.toBytes(request2)}, null);
      ReplicaReply reply2 = deserializeAsReplicaReplier(results2[0]);
      assertNotNull(reply2);
      assertEquals("TOPOLOGY_UPDATED", extractResponse(reply2).content());

      // Third update (should fail - circular)
      List<List<Integer>> topology3 = Arrays.asList(Arrays.asList(0));
      var request3 = TopologyUpdateRequest.create(topology3);
      byte[][] results3 = replicaNode.appExecuteBatch(new byte[][] {Serializer.toBytes(request3)}, null);
      ReplicaReply reply3 = deserializeAsReplicaReplier(results3[0]);
      assertNotNull(reply3);
      assertTrue(extractResponse(reply3).content().startsWith("TOPOLOGY_UPDATE_FAILED:"));
    }
  }

  @Nested
  @DisplayName("Regular Request Response Serialization")
  class RegularRequestResponseSerialization {

    @Test
    @DisplayName("Regular client request returns ReplicaReply that can be deserialized by ReplicaReplier")
    void regularClientRequestReturnsDeserializableReplicaReply() throws Exception {
      var request = new Request(UUID.randomUUID(), new int[] {0}, "test-payload", Request.Source.CLIENT);
      byte[] serializedRequest = Serializer.toBytes(request);

      byte[][] results = replicaNode.appExecuteBatch(new byte[][] {serializedRequest}, null);
      assertEquals(1, results.length);

      // Verify the response can be deserialized as ReplicaReply
      ReplicaReply reply = deserializeAsReplicaReplier(results[0]);
      assertNotNull(reply, "Reply should not be null");
    }

    @Test
    @DisplayName("Invalid payload returns ReplicaReply that can be deserialized by ReplicaReplier")
    void invalidPayloadReturnsDeserializableReplicaReply() throws Exception {
      byte[] invalidPayload = "not a valid serialized object".getBytes();

      byte[][] results = replicaNode.appExecuteBatch(new byte[][] {invalidPayload}, null);
      assertEquals(1, results.length);

      // Verify the response can be deserialized as ReplicaReply
      ReplicaReply reply = deserializeAsReplicaReplier(results[0]);
      assertNotNull(reply, "Reply should not be null");
      assertInstanceOf(ReplicaReply.Raw.class, reply, "Reply should be Raw type");

      // Verify the response indicates invalid payload
      Response response = extractResponse(reply);
      assertEquals("INVALID_PAYLOAD", response.content());
    }
  }

  @Nested
  @DisplayName("Unordered Request Response Serialization")
  class UnorderedRequestResponseSerialization {

    @Test
    @DisplayName("Unordered request returns ReplicaReply that can be deserialized by ReplicaReplier")
    void unorderedRequestReturnsDeserializableReplicaReply() throws Exception {
      byte[] request = "any request".getBytes();

      byte[] result = replicaNode.appExecuteUnordered(request, null);
      assertNotNull(result);

      // Verify the response can be deserialized as ReplicaReply
      ReplicaReply reply = deserializeAsReplicaReplier(result);
      assertNotNull(reply, "Reply should not be null");
      assertInstanceOf(ReplicaReply.Raw.class, reply, "Reply should be Raw type");

      // Verify the response indicates unsupported operation
      Response response = extractResponse(reply);
      assertEquals("UNSUPPORTED_OPERATION", response.content());
    }
  }

  @Nested
  @DisplayName("Batch Processing")
  class BatchProcessing {

    @Test
    @DisplayName("Batch of mixed requests all return deserializable ReplicaReplies")
    void batchOfMixedRequestsReturnsDeserializableReplies() throws Exception {
      // Create a batch with topology update and regular request
      List<List<Integer>> newTopology = Arrays.asList(Arrays.asList(1), Arrays.asList());
      var topologyRequest = TopologyUpdateRequest.create(newTopology);

      var regularRequest = new Request(UUID.randomUUID(), new int[] {0}, "test", Request.Source.CLIENT);

      byte[][] commands = new byte[][] {
          Serializer.toBytes(topologyRequest),
          Serializer.toBytes(regularRequest)
      };

      byte[][] results = replicaNode.appExecuteBatch(commands, null);
      assertEquals(2, results.length);

      // Both responses should be deserializable as ReplicaReply
      ReplicaReply reply1 = deserializeAsReplicaReplier(results[0]);
      assertNotNull(reply1);
      assertEquals("TOPOLOGY_UPDATED", extractResponse(reply1).content());

      ReplicaReply reply2 = deserializeAsReplicaReplier(results[1]);
      assertNotNull(reply2);
      // The second request might be rejected due to UPDATING state, or processed
      // depending on timing, but it should still be a valid ReplicaReply
    }

    @Test
    @DisplayName("Batch with invalid requests still returns deserializable ReplicaReplies")
    void batchWithInvalidRequestsReturnsDeserializableReplies() throws Exception {
      byte[][] commands = new byte[][] {
          "invalid1".getBytes(),
          "invalid2".getBytes(),
          "invalid3".getBytes()
      };

      byte[][] results = replicaNode.appExecuteBatch(commands, null);
      assertEquals(3, results.length);

      // All responses should be deserializable as ReplicaReply
      for (int i = 0; i < results.length; i++) {
        ReplicaReply reply = deserializeAsReplicaReplier(results[i]);
        assertNotNull(reply, "Reply " + i + " should not be null");
        assertInstanceOf(ReplicaReply.Raw.class, reply, "Reply " + i + " should be Raw type");
        assertEquals("INVALID_PAYLOAD", extractResponse(reply).content());
      }
    }
  }

  @Nested
  @DisplayName("TopologyManager State After Updates")
  class TopologyManagerStateAfterUpdates {

    @Test
    @DisplayName("Successful topology update changes TopologyManager state correctly")
    void successfulUpdateChangesTopologyManagerState() throws Exception {
      assertEquals(TopologyState.READY, topologyManager.getState());
      assertEquals(3, topologyManager.getTopology().getGroupIDs().size()); // Initial: 0, 1, 2

      // Update topology
      List<List<Integer>> newTopology = Arrays.asList(
          Arrays.asList(1, 2, 3),
          Arrays.asList(),
          Arrays.asList(),
          Arrays.asList()
      );
      var updateRequest = TopologyUpdateRequest.create(newTopology);
      byte[][] results = replicaNode.appExecuteBatch(new byte[][] {Serializer.toBytes(updateRequest)}, null);

      // Verify successful response
      ReplicaReply reply = deserializeAsReplicaReplier(results[0]);
      assertEquals("TOPOLOGY_UPDATED", extractResponse(reply).content());

      // Verify TopologyManager state
      assertEquals(TopologyState.READY, topologyManager.getState());
      assertEquals(4, topologyManager.getTopology().getGroupIDs().size()); // New: 0, 1, 2, 3
    }

    @Test
    @DisplayName("Failed topology update puts TopologyManager in ERROR state")
    void failedUpdatePutsTopologyManagerInErrorState() throws Exception {
      assertEquals(TopologyState.READY, topologyManager.getState());

      // Update with invalid topology
      List<List<Integer>> invalidTopology = Arrays.asList(Arrays.asList(0));
      var updateRequest = TopologyUpdateRequest.create(invalidTopology);
      byte[][] results = replicaNode.appExecuteBatch(new byte[][] {Serializer.toBytes(updateRequest)}, null);

      // Verify failure response
      ReplicaReply reply = deserializeAsReplicaReplier(results[0]);
      assertTrue(extractResponse(reply).content().startsWith("TOPOLOGY_UPDATE_FAILED:"));

      // Verify TopologyManager is in ERROR state
      assertEquals(TopologyState.ERROR, topologyManager.getState());
    }
  }
}
