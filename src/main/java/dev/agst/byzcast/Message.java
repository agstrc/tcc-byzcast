package dev.agst.byzcast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.UUID;

public class Message implements Serializable {
    UUID id = UUID.randomUUID();
    String message;

    public Message(String message) {
        this.message = message;
    }

    static Message fromBytes(byte[] bytes) throws ClassNotFoundException {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStream(bis);

            Object obj = ois.readObject();
            if (obj instanceof Message) {
                return (Message) obj;
            } else {
                throw new ClassNotFoundException(
                        "Object " + obj.getClass().getName() + " is not an instance of Message");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    byte[] toBytes() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(this);
            out.flush();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
