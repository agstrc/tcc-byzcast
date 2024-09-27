package dev.agst.byzcast.message;

import java.io.Serializable;

/**
 * Represents a batch of responses within the ByzCast system.
 *
 * <p>This record encapsulates an array of {@link Response} objects, allowing multiple responses to
 * be processed or transmitted together as a single unit.
 *
 * <p>The {@code BatchResponse} record is characterized by the following attribute:
 *
 * <ul>
 *   <li>{@code responses} - An array of {@link Response} objects representing the individual
 *       responses within the batch.
 * </ul>
 *
 * <p>This record implements the {@link Serializable} interface, enabling it to be serialized for
 * network transmission or persistent storage.
 */
public record BatchResponse(Response[] responses) implements Serializable {}
