package dev.agst.byzcast.node;

import dev.agst.byzcast.group.GroupProxyRetriever;
import dev.agst.byzcast.message.MessageDeserializationException;
import dev.agst.byzcast.message.Request;
import dev.agst.byzcast.message.Response;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;

class RequestHandler {
  private final Logger logger = System.getLogger(RequestHandler.class.getName());

  private final int groupID;

  public int getGroupID() {
    return groupID;
  }

  private final GroupProxyRetriever proxyRetriever;

  public RequestHandler(int groupID, GroupProxyRetriever proxyRetriever) {
    this.groupID = groupID;
    this.proxyRetriever = proxyRetriever;
  }

  public Response handleRequest(Request request) {
    logger.log(Level.INFO, "Got request: " + request.getID().toString());

    if (request.getTargetGroupID() != this.groupID) {
      logger.log(Level.INFO, "Request is for group " + request.getTargetGroupID());
      logger.log(Level.DEBUG, "My group ID is " + this.groupID);

      var proxy = this.proxyRetriever.forGroup(request.getTargetGroupID());
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
