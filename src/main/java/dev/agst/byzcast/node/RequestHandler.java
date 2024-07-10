package dev.agst.byzcast.node;

import dev.agst.byzcast.group.GroupMap;
import dev.agst.byzcast.group.GroupProxyRetriever;
import dev.agst.byzcast.message.MessageDeserializationException;
import dev.agst.byzcast.message.Request;
import dev.agst.byzcast.message.Response;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;

/**
 * Manages the processing of {@link Request} objects directed at the current {@link Node}. This
 * component is responsible for handling the lifecycle of a request within the node's context. It
 * assumes that incoming data has already been converted from bytes to {@link Request} objects
 * upstream, and similarly, it expects that the {@link Response} objects it generates will be
 * converted back into bytes downstream for transmission.
 */
public class RequestHandler {
  private final Logger logger = System.getLogger(RequestHandler.class.getName());

  private final GroupMap groupMap;
  private final int groupID;

  /**
   * Retrieves the group ID associated with this handler.
   *
   * @return The group ID.
   */
  public int getGroupID() {
    return groupID;
  }

  private final GroupProxyRetriever proxyRetriever;

  /**
   * Constructs a new {@code RequestHandler} with the specified group ID, group map, and proxy
   * retriever.
   *
   * @param groupID The group ID associated with this handler.
   * @param groupMap The group map used to navigate the ByzCast network topology.
   * @param proxyRetriever The proxy retriever used to obtain proxies for forwarding requests.
   */
  public RequestHandler(int groupID, GroupMap groupMap, GroupProxyRetriever proxyRetriever) {
    this.groupID = groupID;
    this.groupMap = groupMap;
    this.proxyRetriever = proxyRetriever;
  }

  /**
   * Handles an incoming request. If the request is intended for this handler's group, it is
   * processed locally. Otherwise, it is forwarded to the appropriate group.
   *
   * @param request The request to handle.
   * @return A response to the request.
   */
  public Response handleRequest(Request request) {
    logger.log(Level.INFO, "Got request: " + request.getID().toString());

    if (request.getTargetGroupID() != this.groupID) {
      logger.log(Level.INFO, "Request is for group " + request.getTargetGroupID());
      logger.log(Level.DEBUG, "My group ID is " + this.groupID);

      var nextGroup = this.groupMap.nextGroup(this.groupID, request.getTargetGroupID());
      if (nextGroup.isEmpty()) {
        var response = new Response("NO_ROUTE", this.groupID);
        return response;
      }

      var proxy = this.proxyRetriever.forGroup(nextGroup.get());
      var responseBytes = proxy.invokeOrdered(request.toBytes());

      Response response;
      try {
        response = Response.fromBytes(responseBytes);
      } catch (MessageDeserializationException e) {
        throw new RuntimeException(e); // must be handled upstream
      }

      response.addTargetGroupID(this.groupID);
      return response;
    }

    var response = new Response("OK", this.groupID);
    return response;
  }
}
