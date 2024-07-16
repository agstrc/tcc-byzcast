package dev.agst.byzcast.topology;

import java.util.List;

/**
 * Represents a group in the topology of the ByzCast system.
 *
 * <p>This record encapsulates a group within the system, identified by an {@code id}, and maintains
 * a list of child groups, representing the hierarchical structure of the system. Each group can
 * have zero or more children, allowing for the construction of a tree-like structure that reflects
 * the organization of the system's components or nodes.
 *
 * @param id The unique identifier of the group.
 * @param children A list of child {@code Group} instances.
 */
public record Group(int id, List<Group> children) {}
