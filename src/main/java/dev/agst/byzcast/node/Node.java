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

  /**
   * Constructs a new {@code Node} with the specified request handler.
   *
   * @param handler The request handler responsible for processing incoming requests.
   */
  public Node(RequestHandler handler) {
    this.handler = handler;
  }

  /**
   * Executes a batch of requests, processing each request in the batch and returning the responses
   * as an array of byte arrays.
   *
   * @param requests The batch of requests to execute, represented as an array of byte arrays.
   * @param contexts The contexts associated with each request, provided by the BFT-SMaRt library.
   * @return An array of byte arrays representing the responses to the executed requests.
   */
  @Override
  public byte[][] appExecuteBatch(byte[][] requets, MessageContext[] contexts) {
    return Arrays.stream(requets).map(this::executeRequest).toArray(byte[][]::new);
  }

  /**
   * Executes a single request, deserializing it from a byte array, processing it, and serializing
   * the response back into a byte array.
   *
   * @param requestBytes The request to execute, represented as a byte array.
   * @return The response to the request, serialized as a byte array.
   */
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

  /**
   * Captures the current state of the node, including all handled requests, and serializes it into
   * a byte array.
   *
   * @return A byte array representing the current state of the node.
   */
  @Override
  public byte[] getSnapshot() {
    try {
      var arrayStream = new ByteArrayOutputStream();
      var objectStream = new ObjectOutputStream(arrayStream);

      objectStream.writeObject(this.handledRequests);
      objectStream.flush();
      return arrayStream.toByteArray();
    } catch (Exception e) {
      logger.log(Logger.Level.ERROR, "Error getting snapshot: " + e.getMessage());
      throw new RuntimeException(e);
    }
  }

  /**
   * Restores the state of the node from a given snapshot, deserializing the state from a byte array
   * and updating the node's state accordingly.
   *
   * @param state The snapshot from which to restore the node's state, represented as a byte array.
   */
  @Override
  public void installSnapshot(byte[] state) {
    try {
      var arrayStream = new ByteArrayInputStream(state);
      var objectStream = new ObjectInputStream(arrayStream);

      @SuppressWarnings("unchecked")
      var snapshot = (ArrayList<Request>) objectStream.readObject();

      this.handledRequests.clear();
      this.handledRequests.addAll(snapshot);
    } catch (Exception e) {
      logger.log(Logger.Level.ERROR, "Error installing snapshot: " + e.getMessage());
      throw new RuntimeException(e);
    }
  }
}
