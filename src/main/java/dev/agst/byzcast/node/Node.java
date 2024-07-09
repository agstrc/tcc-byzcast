package dev.agst.byzcast.node;

import bftsmart.tom.MessageContext;
import bftsmart.tom.server.defaultservices.DefaultRecoverable;
import dev.agst.byzcast.message.MessageDeserializationException;
import dev.agst.byzcast.message.Request;
import dev.agst.byzcast.message.Response;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.System.Logger;
import java.util.ArrayList;
import java.util.Arrays;

public class Node extends DefaultRecoverable {
  private final Logger logger = System.getLogger(Node.class.getName());

  private final ArrayList<Request> handledRequests = new ArrayList<>();
  private final RequestHandler handler;

  public Node(RequestHandler handler) {
    this.handler = handler;
  }

  @Override
  public byte[][] appExecuteBatch(byte[][] requets, MessageContext[] contexts) {
    return Arrays.stream(requets).map(this::executeRequest).toArray(byte[][]::new);
  }

  private byte[] executeRequest(byte[] requestBytes) {
    Request request;
    try {
      request = Request.fromBytes(requestBytes);
    } catch (MessageDeserializationException e) {
      logger.log(Logger.Level.ERROR, "Error deserializing request: " + e.getMessage());

      var response = new Response("INVALID_REQUEST", this.handler.getGroupID());
      return response.toBytes();
    }

    this.handledRequests.add(request);

    try {
      var response = this.handler.handleRequest(request);
      return response.toBytes();
    } catch (Exception e) {
      logger.log(Logger.Level.ERROR, "Error handling request: " + e.getMessage());

      var response = new Response("INTERNAL_ERROR", this.handler.getGroupID());
      return response.toBytes();
    }
  }

  @Override
  public byte[] appExecuteUnordered(byte[] request, MessageContext argctx) {
    var response = new Response("UNORDERED_NOT_SUPPORTED", this.handler.getGroupID());
    return response.toBytes();
  }

  @Override
  public byte[] getSnapshot() {
    try {
      var arrayStream = new ByteArrayOutputStream();
      var objectStream = new ObjectOutputStream(arrayStream);

      objectStream.writeObject(this);
      objectStream.flush();
      return arrayStream.toByteArray();
    } catch (Exception e) {
      logger.log(Logger.Level.ERROR, "Error getting snapshot: " + e.getMessage());
      throw new RuntimeException(e);
    }
  }

  @Override
  public void installSnapshot(byte[] state) {
    try {
      var arrayStream = new ByteArrayInputStream(state);
      var objectStream = new ObjectInputStream(arrayStream);

      var snapshot = (Node) objectStream.readObject();
      this.handledRequests.clear();
      this.handledRequests.addAll(snapshot.handledRequests);
    } catch (Exception e) {
      logger.log(Logger.Level.ERROR, "Error installing snapshot: " + e.getMessage());
      throw new RuntimeException(e);
    }
  }
}
