package dev.agst.byzcast.message;

import java.io.Serializable;

public record Response(String content, int[] groupIDs) implements Serializable {}
