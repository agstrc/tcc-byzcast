package dev.agst.byzcast.api;

import bftsmart.tom.ServiceProxy;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import dev.agst.byzcast.Logger;
import dev.agst.byzcast.Logger.Attr;
import dev.agst.byzcast.Serializer;
import dev.agst.byzcast.message.Response;
import dev.agst.byzcast.message.TopologyUpdateRequest;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 * An embedded HTTP server that provides an endpoint for updating the topology.
 *
 * <p>This server exposes a POST /topology endpoint that accepts a JSON body containing the new
 * topology configuration. When a valid topology is received, it is broadcast to all replicas via
 * BFT-SMaRT consensus.
 *
 * <p>The topology JSON format is a list of lists where each inner list contains the children group
 * IDs for the group at that index:
 *
 * <pre>{@code
 * [
 *   [1, 2],  // Group 0 has children 1 and 2
 *   [3],     // Group 1 has child 3
 *   [],      // Group 2 has no children
 *   []       // Group 3 has no children
 * ]
 * }</pre>
 */
public class TopologyHttpServer {
  private final Server server;
  private final Logger logger;
  private final ServiceProxy proxy;
  private final Gson gson;

  /**
   * Creates a new TopologyHttpServer.
   *
   * @param port The port to listen on.
   * @param proxy The ServiceProxy to use for broadcasting topology updates via BFT-SMaRT.
   * @param logger The logger to use for logging.
   */
  public TopologyHttpServer(int port, ServiceProxy proxy, Logger logger) {
    this.server = new Server(port);
    this.proxy = proxy;
    this.logger = logger;
    this.gson = new Gson();

    setupServlet();
  }

  private void setupServlet() {
    var context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath("/");
    server.setHandler(context);

    context.addServlet(new ServletHolder(new TopologyServlet()), "/topology");
  }

  /**
   * Starts the HTTP server.
   *
   * @throws Exception If the server fails to start.
   */
  public void start() throws Exception {
    server.start();
    logger.info("Topology HTTP server started", new Attr("port", server.getURI().getPort()));
  }

  /**
   * Stops the HTTP server.
   *
   * @throws Exception If the server fails to stop.
   */
  public void stop() throws Exception {
    server.stop();
    logger.info("Topology HTTP server stopped");
  }

  /**
   * Checks if the server is running.
   *
   * @return true if the server is running, false otherwise.
   */
  public boolean isRunning() {
    return server.isRunning();
  }

  /**
   * Servlet that handles topology update requests.
   */
  private class TopologyServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
      resp.setContentType("application/json");

      try {
        var body = new String(req.getInputStream().readAllBytes());
        logger.info("Received topology update request", new Attr("body", body));

        var type = new TypeToken<List<List<Integer>>>() {}.getType();
        List<List<Integer>> topologyList;

        try {
          topologyList = gson.fromJson(body, type);
        } catch (JsonSyntaxException e) {
          logger.error("Invalid JSON in topology update request", e);
          resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
          resp.getWriter().write("{\"error\": \"Invalid JSON format\"}");
          return;
        }

        if (topologyList == null || topologyList.isEmpty()) {
          logger.error("Empty topology in update request");
          resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
          resp.getWriter().write("{\"error\": \"Topology cannot be null or empty\"}");
          return;
        }

        var updateRequest = TopologyUpdateRequest.create(topologyList);
        logger.info("Broadcasting topology update via BFT-SMaRT", new Attr("requestId", updateRequest.id()));

        byte[] responseBytes;
        try {
          responseBytes = proxy.invokeOrdered(Serializer.toBytes(updateRequest));
        } catch (Exception e) {
          logger.error("Failed to broadcast topology update", e);
          resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
          resp.getWriter().write("{\"error\": \"Failed to broadcast topology update: " + e.getMessage() + "\"}");
          return;
        }

        var response = Serializer.fromBytes(responseBytes, Response.class);
        logger.info("Topology update completed", new Attr("response", response.content()));

        if ("TOPOLOGY_UPDATED".equals(response.content())) {
          resp.setStatus(HttpServletResponse.SC_OK);
          resp.getWriter().write("{\"status\": \"success\", \"message\": \"Topology updated successfully\"}");
        } else {
          resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
          resp.getWriter().write("{\"error\": \"Topology update failed: " + response.content() + "\"}");
        }

      } catch (Exception e) {
        logger.error("Unexpected error processing topology update", e);
        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        resp.getWriter().write("{\"error\": \"Unexpected error: " + e.getMessage() + "\"}");
      }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
      resp.setContentType("application/json");
      resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
      resp.getWriter().write("{\"error\": \"Use POST method to update topology\"}");
    }
  }
}
