package dev.agst.byzcast.server;

import bftsmart.tom.ServiceProxy;
import dev.agst.byzcast.Serializer;
import dev.agst.byzcast.SerializingException;
import dev.agst.byzcast.message.Request.ReplicaRequest;
import dev.agst.byzcast.message.Response.BatchResponse;

/**
 * Handles the dispatching of requests from one server to another within the ByzCast protocol.
 *
 * <p>This class is responsible for serializing {@link ReplicaRequest} instances, sending them
 * through a {@link ServiceProxy}, and deserializing the received responses into {@link
 * BatchResponse} instances.
 */
class RequestDispatcher {
  private ServiceProxy proxy;

  /**
   * Constructs a new {@code RequestDispatcher} with the specified {@code ServiceProxy}.
   *
   * @param proxy The {@code ServiceProxy} used to send requests.
   */
  RequestDispatcher(ServiceProxy proxy) {
    this.proxy = proxy;
  }

  /**
   * Dispatches a {@link ReplicaRequest} to another server and returns the {@link BatchResponse}.
   *
   * <p>This method serializes the {@code ReplicaRequest}, sends it using the {@code ServiceProxy},
   * and deserializes the received response into a {@code BatchResponse}.
   *
   * @param request The {@code ReplicaRequest} to be dispatched.
   * @return The {@code BatchResponse} received from the server.
   * @throws RuntimeException If an error occurs during serialization or deserialization.
   */
  public BatchResponse dispatch(ReplicaRequest request) {
    var payload = Serializer.toBytes(request);
    var rawResponse = this.proxy.invokeOrdered(payload);

    try {
      return Serializer.fromBytes(rawResponse, BatchResponse.class);
    } catch (SerializingException e) {
      throw new RuntimeException(e);
    }
  }
}
