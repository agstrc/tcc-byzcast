package dev.agst.byzcast.server;

import bftsmart.tom.MessageContext;
import bftsmart.tom.ReplicaContext;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.server.Replier;
import dev.agst.byzcast.Logger;
import dev.agst.byzcast.Serializer;
import dev.agst.byzcast.SerializingException;
import dev.agst.byzcast.message.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ServerReplier implements Replier {
  private final Logger logger = new Logger();

  private final Lock replyLock = new ReentrantLock();
  private final Condition isContextSet = replyLock.newCondition();

  private ReplicaContext replicaContext;

  private Map<UUID, List<TOMMessage>> pending = new TreeMap<>();

  @Override
  public void manageReply(TOMMessage msg, MessageContext ctx) {
    waitForContext();

    var rawReply = msg.reply.getContent();
    ServerReply reply;
    try {
      reply = Serializer.fromBytes(rawReply, ServerReply.class);
    } catch (SerializingException e) {
      logger.error("Replier could not deserialize reply", e);

      var response = new Response.AggregatedResponse("DESERIALIZATION_ERROR", new ArrayList<>());
      msg.reply.setContent(Serializer.toBytes(response));
      replicaContext.getServerCommunicationSystem().send(new int[] {msg.getSender()}, msg.reply);
      return;
    }

    switch (reply) {
      case ServerReply.Pending pending:
        {
          this.pending.computeIfAbsent(pending.id(), k -> new ArrayList<>()).add(msg);
          break;
        }
      case ServerReply.Done completed:
        {
          msg.reply.setContent(completed.content().clone());
          replicaContext
              .getServerCommunicationSystem()
              .send(new int[] {msg.getSender()}, msg.reply);
          break;
        }
    }
  }

  public void sendReply(UUID id, byte[] content) {
    waitForContext();

    var pendingList = this.pending.remove(id);
    if (pendingList == null) pendingList = new ArrayList<>();
    System.out.println("Pending list size: " + pendingList.size());

    pendingList.forEach(
        pendingMessage -> {
          pendingMessage.reply.setContent(content.clone());
          replicaContext
              .getServerCommunicationSystem()
              .send(new int[] {pendingMessage.getSender()}, pendingMessage.reply);
        });
  }

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

  @Override
  public void setReplicaContext(ReplicaContext rc) {
    replyLock.lock();
    this.replicaContext = rc;
    isContextSet.signalAll();
    replyLock.unlock();
  }
}
