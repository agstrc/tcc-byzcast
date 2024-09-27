package dev.agst.byzcast.v2.replica_;

import bftsmart.tom.MessageContext;
import bftsmart.tom.ReplicaContext;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.server.Replier;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

// public class ReplicaReplier implements Replier {
// //   private final Lock replyLock = new ReentrantLock();
// //   private final Condition isContextSet = replyLock.newCondition();

// //   private ReplicaContext replicaContext;

// //   @Override
// //   public void manageReply(TOMMessage msg, MessageContext ctx) {
// //     lock();
// //   }

// //   private void lock() {
// //     while (this.replicaContext == null) {
// //       try {
// //         replyLock.lock();
// //         isContextSet.await();
// //         replyLock.unlock();
// //       } catch (InterruptedException e) {
// //         throw new RuntimeException("Interrupted while waiting for context to be set", e);
// //       }
// //     }
// //   }
// }
