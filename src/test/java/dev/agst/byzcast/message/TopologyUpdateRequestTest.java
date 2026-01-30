package dev.agst.byzcast.message;

import static org.junit.jupiter.api.Assertions.*;

import dev.agst.byzcast.Serializer;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link TopologyUpdateRequest}. */
class TopologyUpdateRequestTest {

  @Test
  void testCreateWithTopology() {
    List<List<Integer>> topology = Arrays.asList(Arrays.asList(1, 2), Arrays.asList(), Arrays.asList());

    var request = TopologyUpdateRequest.create(topology);

    assertNotNull(request);
    assertNotNull(request.id());
    assertEquals(topology, request.topology());
  }

  @Test
  void testCreateGeneratesUniqueIds() {
    List<List<Integer>> topology = Arrays.asList(Arrays.asList(1), Arrays.asList());

    var request1 = TopologyUpdateRequest.create(topology);
    var request2 = TopologyUpdateRequest.create(topology);

    assertNotEquals(request1.id(), request2.id());
  }

  @Test
  void testRecordConstructor() {
    UUID id = UUID.randomUUID();
    List<List<Integer>> topology = Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(), Arrays.asList(), Arrays.asList());

    var request = new TopologyUpdateRequest(id, topology);

    assertEquals(id, request.id());
    assertEquals(topology, request.topology());
  }

  @Test
  void testSerializationAndDeserialization() throws Exception {
    List<List<Integer>> topology = Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3), Arrays.asList(), Arrays.asList());

    var originalRequest = TopologyUpdateRequest.create(topology);
    byte[] serialized = Serializer.toBytes(originalRequest);

    assertNotNull(serialized);
    assertTrue(serialized.length > 0);

    var deserializedRequest = Serializer.fromBytes(serialized, TopologyUpdateRequest.class);

    assertEquals(originalRequest.id(), deserializedRequest.id());
    assertEquals(originalRequest.topology(), deserializedRequest.topology());
  }

  @Test
  void testEquality() {
    UUID id = UUID.randomUUID();
    List<List<Integer>> topology = Arrays.asList(Arrays.asList(1), Arrays.asList());

    var request1 = new TopologyUpdateRequest(id, topology);
    var request2 = new TopologyUpdateRequest(id, topology);

    assertEquals(request1, request2);
    assertEquals(request1.hashCode(), request2.hashCode());
  }

  @Test
  void testEmptyTopology() {
    List<List<Integer>> emptyTopology = Arrays.asList(Arrays.asList());

    var request = TopologyUpdateRequest.create(emptyTopology);

    assertNotNull(request);
    assertEquals(emptyTopology, request.topology());
  }

  @Test
  void testComplexTopology() {
    // Complex topology with many groups
    List<List<Integer>> complexTopology =
        Arrays.asList(
            Arrays.asList(1, 2, 3),
            Arrays.asList(4, 5),
            Arrays.asList(6),
            Arrays.asList(7, 8, 9),
            Arrays.asList(),
            Arrays.asList(),
            Arrays.asList(),
            Arrays.asList(),
            Arrays.asList(),
            Arrays.asList());

    var request = TopologyUpdateRequest.create(complexTopology);

    assertNotNull(request);
    assertEquals(10, request.topology().size());
    assertEquals(Arrays.asList(1, 2, 3), request.topology().get(0));
    assertEquals(Arrays.asList(4, 5), request.topology().get(1));
  }
}
