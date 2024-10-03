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

public class ServerReplier implements Replier {
  private final Logger logger = new Logger();

  private final Lock replyLock = new ReentrantLock();
  private final Condition isContextSet = replyLock.newCondition();

  private ReplicaContext replicaContext;

  private final Map<UUID, List<TOMMessage>> pending = new TreeMap<>();

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
