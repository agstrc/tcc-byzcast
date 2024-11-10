package dev.agst.byzcast.server;

import bftsmart.tom.MessageContext;
import bftsmart.tom.ReplicaContext;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.server.Replier;
import dev.agst.byzcast.Logger;
import dev.agst.byzcast.Logger.Attr;
import dev.agst.byzcast.Serializer;
import dev.agst.byzcast.SerializingException;
import dev.agst.byzcast.server.ServerReply.Done;
import dev.agst.byzcast.server.ServerReply.Pending;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Handles the replies for the server within the ByzCast protocol.
 *
 * <p>The {@code ServerReplier} class implements the {@link Replier} interface and manages the
 * replies to client requests. It ensures that replies are sent only after the requests have been
 * fully processed. This class works in conjunction with the {@link ServerReply} interface to
 * facilitate deferred replies, ensuring that requests are processed within the constraints of the
 * bft-smart library, but also allowing for replies to be sent once due processing is complete.
 *
 * <p>The {@code ServerReplier} maintains a map of pending requests and uses a lock mechanism to
 * wait for the {@link ReplicaContext} to be set before processing replies. It handles both
 * completed and pending replies, sending the appropriate response to the client once processing is
 * complete.
 */
public class ServerReplier implements Replier {
  private final Logger logger = new Logger();

  private final Lock replyLock = new ReentrantLock();
  private final Condition isContextSet = replyLock.newCondition();

  private ReplicaContext replicaContext;

  private final Map<UUID, List<TOMMessage>> pending = new TreeMap<>();

  /**
   * Manages the reply for a given TOMMessage.
   *
   * <p>This method deserializes the reply content and determines whether the reply is completed or
   * pending. If the reply is pending, it adds the message to the pending list. If the reply is
   * completed, it sends the reply to the client.
   *
   * @param msg the TOMMessage containing the reply
   * @param ctx the MessageContext associated with the message
   */
  @Override
  public void manageReply(TOMMessage msg, MessageContext ctx) {
    waitForContext();

    var replyContent = msg.reply.getContent();
    ServerReply reply;
    try {
      reply = Serializer.fromBytes(replyContent, ServerReply.class);
    } catch (SerializingException e) {
      // TODO: error handling; probably should reply with some sort of error

      logger.error("Failed to deserialize server reply", e);
      return;
    }

    switch (reply) {
      case Pending pending:
        {
          var pendingList = this.pending.computeIfAbsent(pending.id(), k -> new ArrayList<>());
          pendingList.add(msg);
          break;
        }
      case Done done:
        {
          msg.reply.setContent(done.data().clone());
          replicaContext
              .getServerCommunicationSystem()
              .send(new int[] {msg.getSender()}, msg.reply);
          break;
        }
    }
  }

  /**
   * Sends a reply for a pending request.
   *
   * <p>This method retrieves the list of pending messages for the given request ID and sends the
   * reply data to the client. If no pending messages are found for the given ID, it logs an error.
   *
   * @param id the unique identifier of the request
   * @param data the reply data to be sent
   */
  public void sendReply(UUID id, byte[] data) {
    waitForContext();

    var pendingList = this.pending.remove(id);
    if (pendingList == null) {
      logger.error("No pending messages for given ID", new Attr("ID", id));
      return;
    }

    pendingList.forEach(
        pendingMessage -> {
          pendingMessage.reply.setContent(data.clone());
          replicaContext
              .getServerCommunicationSystem()
              .send(new int[] {pendingMessage.getSender()}, pendingMessage.reply);
        });
  }

  /**
   * Waits for the {@link ReplicaContext} to be set.
   *
   * <p>This method uses a lock and condition to wait until the {@code ReplicaContext} is set,
   * ensuring that the context is available before processing replies.
   */
  private void waitForContext() {
    while (this.replicaContext == null) {
      try {
        replyLock.lock();
        isContextSet.await();
        replyLock.unlock();
      } catch (InterruptedException e) {
        throw new RuntimeException("Interrupted while waiting for context to be set", e);
      }
    }
  }

  /**
   * Sets the {@link ReplicaContext} for this replier.
   *
   * <p>This method sets the {@code ReplicaContext} and signals all waiting threads that the context
   * is now available.
   *
   * @param rc the ReplicaContext to be set
   */
  @Override
  public void setReplicaContext(ReplicaContext rc) {
    replyLock.lock();
    this.replicaContext = rc;
    isContextSet.signalAll();
    replyLock.unlock();
  }
}
