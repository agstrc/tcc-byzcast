// package dev.agst.byzcast.replica;

// import bftsmart.tom.MessageContext;
// import bftsmart.tom.server.defaultservices.DefaultRecoverable;
// import dev.agst.byzcast.Logger;
// import dev.agst.byzcast.Serializer;
// import dev.agst.byzcast.message.Response;
// import dev.agst.byzcast.v2.message.Request;
// import dev.agst.byzcast.v2.replica_.ReplicaState;
// import dev.agst.byzcast.v2.replica_.RequestHandler;

// import java.security.MessageDigest;
// import java.security.NoSuchAlgorithmException;
// import java.util.ArrayList;
// import java.util.List;

// /**
//  * Implements a node within the ByzCast system, acting as a controller for serializing and
//  * deserializing requests and responses.
//  *
//  * <p>This class is responsible for:
//  *
//  * <ul>
//  *   <li>Serializing requests to be processed by the system.
//  *   <li>Deserializing responses for the clients.
//  *   <li>Forwarding the logical handling of requests to the {@code RequestHandler} class.
//  *   <li>Ensuring that requests and responses are well integrated with {@code ReplicaReplier} for
//  *       reliable communication.
//  * </ul>
//  *
//  * @see dev.agst.byzcast.replica.RequestHandler
//  * @see dev.agst.byzcast.replica.ReplicaReplier
//  */
// public class ReplicaNode extends DefaultRecoverable {
//   private final Logger logger;
//   private final RequestHandler handler;

//   private ReplicaState state;

//   public ReplicaNode(Logger logger, RequestHandler handler, ReplicaState state) {
//     this.logger = logger;
//     this.handler = handler;
//     this.state = state;
//   }

//   // /**
//   //  * A factory method for creating instances of {@code ReplicaNode}. This method is used to
//   // ensure
//   //  * that all necessary components are properly initialized before the instance is created.
//   //  */
//   // public static ReplicaNodeBuilderFactory.LoggerConfigurator builder() {
//   //   return new ReplicaNodeBuilderFactory.Builder();
//   // }

//   @Override
//   public byte[][] appExecuteBatch(byte[][] cmds, MessageContext[] ctxs) {
//     System.out.println("TOTAL_BATCH_SIZE " + cmds.length);
//     List<Request> requests = new ArrayList<Request>(cmds.length);

//     for (var cmd : cmds) {
//       Request request;
//       try {
//         request = Serializer.fromBytes(cmd, Request.class);
//       } catch (Exception e) {
//         throw new RuntimeException(e);
//       }

//       requests.add(request);
//     }

//     logger.info("Handling", new Logger.Attr("reqs", requests));
//     logger.info("BATCH_MD5", new Logger.Attr("md5", getMD5(requests)));
//     var replies = this.handler.handle2(requests, state);
//     logger.info("Will return as yes", new Logger.Attr("reqs", requests));
//     return replies.stream().map(Serializer::toBytes).toArray(byte[][]::new);
//   }

//   public static String getMD5(List<Request> requests) {
//     try {
//       MessageDigest md = MessageDigest.getInstance("MD5");
//       for (Request request : requests) {
//         md.update(Serializer.toBytes(request));
//       }
//       byte[] digest = md.digest();
//       StringBuilder sb = new StringBuilder();
//       for (byte b : digest) {
//         sb.append(String.format("%02x", b));
//       }
//       return sb.toString();
//     } catch (NoSuchAlgorithmException e) {
//       throw new RuntimeException("MD5 algorithm not found", e);
//     }
//   }

//   // private byte[] appExecuteSingle(byte[] cmd) {
//   // Request request;

//   // try {
//   //   request = Serializer.fromBytes(cmd, Request.class);
//   // } catch (Exception e) {
//   //   logger.error("Failed to deserialize request", e);
//   //   var response = new Response("INVALID_PAYLOAD", new ArrayList<>());
//   //   var reply = new ReplicaReply.Raw(Serializer.toBytes(response));

//   //   return Serializer.toBytes(reply);
//   // }

//   // try {
//   //   ReplicaReply reply = this.handler.handle(request, state);
//   //   return Serializer.toBytes(reply);
//   // } catch (Exception e) {
//   //   logger.error("Failed to handle request", e, new Attr("RID", request.id()));

//   //   var response = new Response("INTERNAL_ERROR", new ArrayList<>());
//   //   var errorReply = new ReplicaReply.Raw(Serializer.toBytes(response));
//   //   return Serializer.toBytes(errorReply);
//   // }
//   // }

//   @Override
//   public byte[] appExecuteUnordered(byte[] cmd, MessageContext ctx) {
//     var response = new Response("UNSUPPORTED_OPERATION", new ArrayList<>());
//     var rawResponse = Serializer.toBytes(response);
//     return Serializer.toBytes(new ReplicaReply.Raw(rawResponse));
//   }

//   @Override
//   public byte[] getSnapshot() {
//     return Serializer.toBytes(this.state);
//   }

//   @Override
//   public void installSnapshot(byte[] state) {
//     try {
//       this.state = Serializer.fromBytes(state, ReplicaState.class);
//     } catch (Exception e) {
//       logger.error("Failed to deserialize snapshot", e);
//       throw new RuntimeException(e);
//     }
//   }
// }
