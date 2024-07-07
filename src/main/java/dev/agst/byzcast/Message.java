package dev.agst.byzcast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.UUID;

import dev.agst.byzcast.exceptions.InvalidMessageException;

/**
 * Represents a message in the ByzCast system.
 */
public class Message implements Serializable {
    UUID id = UUID.randomUUID();

    int groupID;
    String message;

    /**
     * Constructs a new Message object with the given message and group ID.
     *
     * @param message the content of the message
     * @param groupID the ID of the group to which the message belongs
     */
    public Message(String message, int groupID) {
        this.groupID = groupID;
        this.message = message;
    }

    /**
     * Deserializes a Message object from the given byte array.
     *
     * @param bytes the byte array representing the serialized Message object
     * @return the deserialized Message object
     * @throws ClassNotFoundException  if the class of the serialized object cannot
     *                                 be found
     * @throws InvalidMessageException if the deserialized object is not an instance
     *                                 of Message
     */
    static Message fromBytes(byte[] bytes) throws ClassNotFoundException, InvalidMessageException {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStream(bis);

            Object obj = ois.readObject();
            if (obj instanceof Message) {
                return (Message) obj;
            } else {
                var errorMessage = "Object " + obj.getClass().getName() + " is not an instance of Message";
                throw new InvalidMessageException(errorMessage);
            }
        } catch (IOException e) {
            // we don't expect IOException to happen since we are reading from a byte array
            throw new RuntimeException(e);
        }
    }

    /**
     * Serializes the Message object to a byte array.
     *
     * @return the byte array representing the serialized Message object
     */
    byte[] toBytes() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(this);
            out.flush();
            return bos.toByteArray();
        } catch (IOException e) {
            // we don't expect IOException to happen since we are reading from a byte array
            throw new RuntimeException(e);
        }
    }
}
