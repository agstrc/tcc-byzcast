package dev.agst.byzcast;

import java.nio.ByteBuffer;

/**
 * The ResponseMaker class is responsible for generating response messages for a
 * specific group.
 */
public class ResponseMaker {
    private int groupID;

    /**
     * Constructs a new ResponseMaker object with the specified group ID.
     *
     * @param groupID the ID of the group
     */
    ResponseMaker(int groupID) {
        this.groupID = groupID;
    }

    /**
     * Creates a response by concatenating a prefix with the given data.
     *
     * @param data The data to be included in the response.
     * @return The response as a byte array.
     */
    public byte[] makeResponse(byte[] data) {
        var prefix = ("GROUP_" + groupID + "\n").getBytes();

        var buffer = ByteBuffer.allocate(prefix.length + data.length);
        buffer.put(prefix);
        buffer.put(data);
        return buffer.array();
    }
}
