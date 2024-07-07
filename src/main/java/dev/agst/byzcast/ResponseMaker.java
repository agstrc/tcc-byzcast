package dev.agst.byzcast;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class ResponseMaker {
    private int groupID;
    private ObjectMapper objectMapper;

    public ResponseMaker(int groupID) {
        this.groupID = groupID;
        this.objectMapper = new ObjectMapper();
    }

    public byte[] makeResponse(String response) {
        JsonNode responseJson = objectMapper.createObjectNode()
                .put("groupID", groupID)
                .put("response", response);

        return jsonNodeToBytes(responseJson);
    }

    public byte[] wrapResponse(byte[] data) {
        JsonNode jsonNode = null;
        try {
            jsonNode = objectMapper.readTree(data);
        } catch (IOException e) {
            if (!(e instanceof JsonParseException)) {
                throw new RuntimeException(e);
            }
        }

        // if it's valid JSON, we return it wrapped
        if (jsonNode != null) {
            JsonNode responseJson = objectMapper.createObjectNode()
                    .put("groupID", groupID)
                    .set("response", jsonNode);
            return jsonNodeToBytes(responseJson);
        }

        JsonNode responseJson = objectMapper.createObjectNode()
                .put("groupID", groupID)
                .put("response", stringOrBase64(data));
        return jsonNodeToBytes(responseJson);
    }

    static String stringOrBase64(byte[] data) {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
        try {
            ByteBuffer byteBuffer = ByteBuffer.wrap(data);
            return decoder.decode(byteBuffer).toString();
        } catch (CharacterCodingException e) {
            return Base64.getEncoder().encodeToString(data);
        }
    }

    byte[] jsonNodeToBytes(JsonNode jsonNode) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(jsonNode);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
    // try {
    // return jsonNode.toString().getBytes();
    // } catch (JsonProcessingException e) {
    // Logger.getLogger(ResponseMaker.class.getName())
}