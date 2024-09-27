package dev.agst.byzcast.message;

import java.io.Serializable;
import java.util.UUID;

/**
 * Represents a batch of requests within the ByzCast system.
 *
 * <p>This record encapsulates a unique batch identifier and an array of {@link Request} objects,
 * allowing multiple requests to be processed or transmitted together as a single unit.
 *
 * <p>The {@code BatchRequest} record is characterized by the following attributes:
 *
 * <ul>
 *   <li>{@code batchID} - A unique identifier for the batch of requests.
 *   <li>{@code requests} - An array of {@link Request} objects representing the individual requests
 *       within the batch.
 * </ul>
 *
 * <p>This record implements the {@link Serializable} interface, enabling it to be serialized for
 * network transmission or persistent storage.
 */
public record BatchRequest(UUID batchID, Request[] requests) implements Serializable {}
