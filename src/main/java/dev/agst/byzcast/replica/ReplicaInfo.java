package dev.agst.byzcast.replica;

/**
 * Represents the identity of a replica within a group in the ByzCast system. This record holds the
 * group and server identifiers, which uniquely identify a replica.
 *
 * @param groupID The identifier of the group to which the replica belongs.
 * @param serverID The unique identifier of the replica within its group.
 */
public record ReplicaInfo(int groupID, int serverID) {}
