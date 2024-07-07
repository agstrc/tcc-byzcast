package dev.agst.byzcast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.logging.Logger;

import bftsmart.tom.MessageContext;
import bftsmart.tom.server.defaultservices.DefaultRecoverable;

public class MessageServer extends DefaultRecoverable {
    private HashSet<Message> messageSet = new HashSet<>();
    private Logger logger = Logger.getLogger(MessageServer.class.getName());

    @Override
    public byte[][] appExecuteBatch(byte[][] commands, MessageContext[] contexts) {
        return Arrays.stream(commands).map(this::executeCommand).toArray(byte[][]::new);
    }

    private byte[] executeCommand(byte[] command) {
        Message message;

        try {
            message = Message.fromBytes(command);
        } catch (ClassNotFoundException e) {
            logger.severe("Error deserializing message: " + e.getMessage());
            return "INVALID_MESSAGE".getBytes();
        }

        messageSet.add(message);
        return "OK".getBytes();
    }

    @Override
    public byte[] appExecuteUnordered(byte[] cmd, MessageContext ctx) {
        logger.warning("Received non supported unordered request. Ignoring.");
        return "UNORDERED_NOT_SUPPORTED".getBytes();
    }

    @Override
    public byte[] getSnapshot() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(messageSet);
            out.flush();
            return bos.toByteArray();
        } catch (IOException e) {
            logger.severe("Error serializing message set: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void installSnapshot(byte[] state) {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(state);
            ObjectInputStream ois = new ObjectInputStream(bis);

            Object obj = ois.readObject();
            if (obj instanceof HashSet) {
                messageSet = (HashSet<Message>) obj;
            } else {
                throw new ClassNotFoundException(
                        "Object " + obj.getClass().getName() + " is not an instance of HashSet<Message>");
            }

        } catch (IOException | ClassNotFoundException e) {
            logger.severe("Error deserializing message set: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

}