package dev.agst.byzcast.replica;

import bftsmart.tom.MessageContext;
import bftsmart.tom.ReplicaContext;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.server.Replier;
import dev.agst.byzcast.Logger;
import dev.agst.byzcast.Serializer;
import dev.agst.byzcast.SerializingException;
import dev.agst.byzcast.message.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ReplicaReplier implements Replier {
  private final Lock replyLock = new ReentrantLock();
  private final Condition isContextSet = replyLock.newCondition();

  private ReplicaContext replicaContext;

  private Map<UUID, List<TOMMessage>> pendingRequests =
      Collections.synchronizedMap(new HashMap<>());

  @Override
  public void manageReply(TOMMessage msg, MessageContext ctx) {
    while (this.replicaContext == null) {
      try {
        replyLock.lock();
        isContextSet.await();
        replyLock.unlock();
      } catch (InterruptedException e) {
        throw new RuntimeException("Interrupted while waiting for context to be set", e);
      }
    }

    var rawReply = msg.reply.getContent();
    ReplicaReply deserializedReply;
    try {
      deserializedReply = Serializer.fromBytes(rawReply, ReplicaReply.class);
    } catch (SerializingException e) {
      var logger = new Logger();
      logger.error("Replier failed to deserialize reply", e);

      var response = new Response("EXCEPTIONAL_ERROR", null);
      msg.reply.setContent(Serializer.toBytes(response));
      replicaContext.getServerCommunicationSystem().send(new int[] {msg.getSender()}, msg.reply);
      return;
    }

    switch (deserializedReply) {
      case ReplicaReply.Pending pending:
        {
          this.pendingRequests
              .computeIfAbsent(pending.id(), k -> Collections.synchronizedList(new ArrayList<>()))
              .add(msg);
          break;
        }
      case ReplicaReply.Completed completed:
        {
          var pendingList = this.pendingRequests.remove(completed.id());
          pendingList.add(msg);

          pendingList.forEach(
              pendingMessage -> {
                // I'm not sure whether the contents required to be cloned, but this is
                // done in case the library modifies the array buffer
                pendingMessage.reply.setContent(completed.result().clone());
                replicaContext
                    .getServerCommunicationSystem()
                    .send(new int[] {pendingMessage.getSender()}, pendingMessage.reply);
              });
          break;
        }
      case ReplicaReply.Raw raw:
        {
          msg.reply.setContent(raw.data());
          replicaContext
              .getServerCommunicationSystem()
              .send(new int[] {msg.getSender()}, msg.reply);
          break;
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
