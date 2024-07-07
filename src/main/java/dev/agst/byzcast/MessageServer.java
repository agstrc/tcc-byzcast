package dev.agst.byzcast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.logging.Logger;

import bftsmart.tom.MessageContext;
import bftsmart.tom.ServiceProxy;
import bftsmart.tom.server.defaultservices.DefaultRecoverable;
import dev.agst.byzcast.exceptions.InvalidMessageException;
import dev.agst.byzcast.groups.GroupsConfig;
import dev.agst.byzcast.utils.ConfigHomeFinder;

public class MessageServer extends DefaultRecoverable {
    private HashSet<Message> messageSet = new HashSet<>();
    private Logger logger;

    private int groupID;

    private GroupsConfig groupsConfig;
    private HashMap<Integer, ServiceProxy> groupProxies;

    private ResponseMaker responseMaker;

    MessageServer(int groupID, GroupsConfig groupsConfig, ConfigHomeFinder configHomeFinder) {
        this.logger = Logger.getLogger(String.format("Group %d - %s", groupID, MessageServer.class.getName()));

        this.groupID = groupID;
        this.groupsConfig = groupsConfig;

        responseMaker = new ResponseMaker(groupID);

        this.groupProxies = new HashMap<>();
        var random = new Random();
        groupsConfig.getAssociatedGroups(groupID).forEach(associatedGroup -> {
            var cfgHome = configHomeFinder.forGroup(associatedGroup);
            groupProxies.put(associatedGroup, new ServiceProxy(random.nextInt(Integer.MAX_VALUE), cfgHome));
        });
    }

    @Override
    public byte[][] appExecuteBatch(byte[][] commands, MessageContext[] contexts) {
        return Arrays.stream(commands).map(this::executeCommand).toArray(byte[][]::new);
    }

    private byte[] executeCommand(byte[] command) {
        Message message;

        try {
            message = Message.fromBytes(command);
        } catch (ClassNotFoundException | InvalidMessageException e) {
            logger.severe("Error deserializing message: " + e.getMessage());
            return responseMaker.makeResponse("INVALID_MESSAGE".getBytes());
        }

        logger.info("Received message: " + message.id);

        if (message.groupID != groupID) {
            logger.info("Received message for group " + message.groupID);

            var nextGroup = groupsConfig.getNextGroup(groupID, message.groupID);
            if (nextGroup == null) {
                logger.warning("No path to group " + message.groupID);
                return responseMaker.makeResponse("NO_PATH".getBytes());
            }

            var nextGroupProxy = groupProxies.get(nextGroup);
            if (nextGroupProxy == null) {
                logger.severe("No proxy for group " + nextGroup);
                return "NO_PROXY".getBytes();
            }

            return responseMaker.makeResponse(nextGroupProxy.invokeOrdered(command));
        }

        messageSet.add(message);
        return responseMaker.makeResponse("OK".getBytes());
    }

    @Override
    public byte[] appExecuteUnordered(byte[] cmd, MessageContext ctx) {
        logger.warning("Received non supported unordered request. Ignoring.");
        return responseMaker.makeResponse("UNSUPPORTED".getBytes());
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