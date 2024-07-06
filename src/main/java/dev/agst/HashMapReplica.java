package dev.agst;

import java.util.HashMap;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;

import bftsmart.tom.MessageContext;
import bftsmart.tom.server.defaultservices.DefaultRecoverable;

public class HashMapReplica extends DefaultRecoverable {
    private HashMap<String, String> hashMap = new HashMap<>();

    @Override
    public byte[][] appExecuteBatch(byte[][] command, MessageContext[] mcs) {
        // command.stream()
        return Arrays.stream(command).map(cmd -> {
            System.err.println("Received command: " + new String(cmd));
            return "Sucess".getBytes();
        }).toArray(byte[][]::new);
    }

    @Override
    public byte[] getSnapshot() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(this.hashMap);
            oos.flush();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void installSnapshot(byte[] state) {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(state);
            ObjectInputStream ois = new ObjectInputStream(bis);
            Object object = ois.readObject();

            if (object instanceof HashMap) {
                this.hashMap = (HashMap<String, String>) object;
            } else {
                throw new IllegalArgumentException("Invalid object type. Expected HashMap<String, String> but got "
                        + object.getClass().getName() + " instead.");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] appExecuteUnordered(byte[] bytes, MessageContext messageContext) {
        throw new UnsupportedOperationException("Global server only accepts ordered messages");
    }
}
