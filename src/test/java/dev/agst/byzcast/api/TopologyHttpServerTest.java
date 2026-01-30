package dev.agst.byzcast.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import bftsmart.tom.ServiceProxy;
import dev.agst.byzcast.Logger;
import dev.agst.byzcast.Serializer;
import dev.agst.byzcast.message.Response;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link TopologyHttpServer}. */
@ExtendWith(MockitoExtension.class)
class TopologyHttpServerTest {

  private static final int TEST_PORT = 18080;

  @Mock private ServiceProxy mockProxy;

  private TopologyHttpServer server;
  private HttpClient httpClient;
  private Logger logger;

  @BeforeEach
  void setUp() throws Exception {
    logger = new Logger();
    server = new TopologyHttpServer(TEST_PORT, mockProxy, logger);
    server.start();
    httpClient = HttpClient.newHttpClient();
  }

  @AfterEach
  void tearDown() throws Exception {
    if (server != null && server.isRunning()) {
      server.stop();
    }
  }

  @Test
  void testServerStarts() {
    assertTrue(server.isRunning());
  }

  @Test
  void testServerStops() throws Exception {
    server.stop();
    assertFalse(server.isRunning());
  }

  @Test
  void testPostValidTopologyReturnsSuccess() throws Exception {
    // Setup mock to return success response
    var successResponse = new Response("TOPOLOGY_UPDATED", new ArrayList<>());
    when(mockProxy.invokeOrdered(any(byte[].class))).thenReturn(Serializer.toBytes(successResponse));

    var request =
        HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + TEST_PORT + "/topology"))
            .POST(HttpRequest.BodyPublishers.ofString("[[1, 2], [], []]"))
            .header("Content-Type", "application/json")
            .build();

    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(200, response.statusCode());
    assertTrue(response.body().contains("success"));
    verify(mockProxy, times(1)).invokeOrdered(any(byte[].class));
  }

  @Test
  void testPostInvalidJsonReturnsBadRequest() throws Exception {
    var request =
        HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + TEST_PORT + "/topology"))
            .POST(HttpRequest.BodyPublishers.ofString("not valid json"))
            .header("Content-Type", "application/json")
            .build();

    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(400, response.statusCode());
    assertTrue(response.body().contains("Invalid JSON"));
    verify(mockProxy, never()).invokeOrdered(any(byte[].class));
  }

  @Test
  void testPostEmptyTopologyReturnsBadRequest() throws Exception {
    var request =
        HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + TEST_PORT + "/topology"))
            .POST(HttpRequest.BodyPublishers.ofString("[]"))
            .header("Content-Type", "application/json")
            .build();

    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(400, response.statusCode());
    assertTrue(response.body().contains("cannot be null or empty"));
    verify(mockProxy, never()).invokeOrdered(any(byte[].class));
  }

  @Test
  void testPostNullTopologyReturnsBadRequest() throws Exception {
    var request =
        HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + TEST_PORT + "/topology"))
            .POST(HttpRequest.BodyPublishers.ofString("null"))
            .header("Content-Type", "application/json")
            .build();

    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(400, response.statusCode());
    assertTrue(response.body().contains("cannot be null or empty"));
    verify(mockProxy, never()).invokeOrdered(any(byte[].class));
  }

  @Test
  void testGetMethodReturnsMethodNotAllowed() throws Exception {
    var request =
        HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + TEST_PORT + "/topology"))
            .GET()
            .build();

    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(405, response.statusCode());
    assertTrue(response.body().contains("Use POST method"));
  }

  @Test
  void testProxyFailureReturnsInternalError() throws Exception {
    // Setup mock to throw exception
    when(mockProxy.invokeOrdered(any(byte[].class)))
        .thenThrow(new RuntimeException("Connection failed"));

    var request =
        HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + TEST_PORT + "/topology"))
            .POST(HttpRequest.BodyPublishers.ofString("[[1], []]"))
            .header("Content-Type", "application/json")
            .build();

    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(500, response.statusCode());
    assertTrue(response.body().contains("Failed to broadcast"));
  }

  @Test
  void testTopologyUpdateFailedResponse() throws Exception {
    // Setup mock to return failure response
    var failResponse = new Response("TOPOLOGY_UPDATE_FAILED: Invalid structure", new ArrayList<>());
    when(mockProxy.invokeOrdered(any(byte[].class))).thenReturn(Serializer.toBytes(failResponse));

    var request =
        HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + TEST_PORT + "/topology"))
            .POST(HttpRequest.BodyPublishers.ofString("[[1, 2], [], []]"))
            .header("Content-Type", "application/json")
            .build();

    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(500, response.statusCode());
    assertTrue(response.body().contains("Topology update failed"));
  }

  @Test
  void testComplexTopologyStructure() throws Exception {
    // Setup mock to return success response
    var successResponse = new Response("TOPOLOGY_UPDATED", new ArrayList<>());
    when(mockProxy.invokeOrdered(any(byte[].class))).thenReturn(Serializer.toBytes(successResponse));

    // Complex topology: Group 0 -> [1, 2], Group 1 -> [3, 4], Group 2 -> [5]
    String complexTopology = "[[1, 2], [3, 4], [5], [], [], []]";
    var request =
        HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + TEST_PORT + "/topology"))
            .POST(HttpRequest.BodyPublishers.ofString(complexTopology))
            .header("Content-Type", "application/json")
            .build();

    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(200, response.statusCode());
    assertTrue(response.body().contains("success"));
  }
}
