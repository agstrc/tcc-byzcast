package dev.agst.byzcast.topology;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * The {@code Topology} class models the network structure within the ByzCast system, providing
 * various methods to navigate and manipulate the network topology. This includes loading the
 * topology from a JSON file, finding paths between groups, calculating next steps towards target
 * groups, and determining the lowest common ancestor (LCA) of given group IDs.
 */
public class Topology {
  private Group root;

  /**
   * Initializes a {@code Topology} instance with a given root group.
   *
   * @param root The root group of the topology.
   */
  public Topology(Group root) {
    this.root = root;
  }

  /**
   * Retrieves a set of all group IDs present in the topology.
   *
   * @return A {@code Set<Integer>} containing all group IDs.
   */
  public Set<Integer> getGroupIDs() {
    return getGroupIDs(root, new TreeSet<>());
  }

  private Set<Integer> getGroupIDs(Group current, Set<Integer> groupIDs) {
    groupIDs.add(current.id());
    for (var child : current.children()) {
      getGroupIDs(child, groupIDs);
    }
    return groupIDs;
  }

  /**
   * Retrieves the IDs of all children of a group identified by its ID.
   *
   * @param groupID The ID of the group whose children are to be retrieved.
   * @return An {@code Optional} containing a {@code Set<Integer>} of children IDs if the group is
   *     found, or an empty {@code Optional} if the group is not present in the topology.
   */
  public Optional<Set<Integer>> getChildrenIDs(int groupID) {
    var optGroup = this.findGroupByID(root, groupID);
    if (optGroup.isEmpty()) {
      return Optional.empty();
    }

    var group = optGroup.get();
    Set<Integer> childrenIDs = new HashSet<>();
    for (var child : group.children()) {
      childrenIDs.add(child.id());
    }

    return Optional.of(childrenIDs);
  }

  /**
   * Constructs a {@code Topology} by loading its structure from a JSON file located at the
   * specified path.
   *
   * @param path The file path to the JSON file containing the topology data.
   * @throws TopologyLoadException If there is an error loading the topology from the JSON file.
   */
  public Topology(String path) throws TopologyLoadException {
    try {
      this.root = TopologyLoader.loadFromJSON(path);
    } catch (Exception e) {
      throw new TopologyLoadException(e);
    }
  }

  /**
   * Finds the next group in the adjacency list from the start group to the target group.
   *
   * @param startID The starting group.
   * @param targetID The target group.
   * @return An Optional containing the next group towards the target, or an empty Optional if not
   *     found.
   */
  public Optional<Integer> nextGroup(int startID, int targetID) {
    var path = findPath(startID, targetID);
    if (path.isEmpty()) {
      return Optional.empty(); // Path not found
    }

    var pathList = path.get();
    if (pathList.size() < 2) {
      return Optional.empty(); // No next group
    }

    return Optional.of(pathList.get(1)); // Next group
  }

  /**
   * Calculates the immediate next steps required to reach each target group from a starting group.
   * This method maps each target group to an intermediary group that must be traversed next on the
   * path to the target. Essentially, for every target group, it identifies the next group in the
   * sequence towards reaching that target. This helps in planning a route that covers all specified
   * targets by indicating the next move from the start group.
   *
   * @param startID The starting group from which paths to target groups are sought.
   * @param targetIDs A list of target groups for which paths need to be identified.
   * @return An Optional containing a map, where each key is an intermediary group that leads
   *     towards one or more target groups listed in its associated ArrayList. If the targetIDs list
   *     is empty, or any group is unreachable from the start, returns an empty Optional.
   */
  public Optional<Map<Integer, List<Integer>>> findPaths(int startID, List<Integer> targetIDs) {
    if (targetIDs.isEmpty()) {
      return Optional.empty(); // Return empty if no target groups
    }

    Map<Integer, List<Integer>> paths = new TreeMap<>();
    for (var target : targetIDs) {
      if (target == startID) {
        return Optional.empty(); // Return empty if target is the same as start group.
      }

      var nextGroup = nextGroup(startID, target);
      if (nextGroup.isEmpty()) {
        return Optional.empty();
      }

      var groupsFrom = paths.computeIfAbsent(nextGroup.get(), key -> new ArrayList<>());
      groupsFrom.add(target);
    }

    return Optional.of(paths);
  }

  /**
   * Finds the lowest common ancestor (LCA) of a set of groups identified by their IDs.
   *
   * @param ids A list of integers representing the IDs of the groups for which to find the LCA.
   * @return An {@code Optional<Group>} containing the LCA group if found, or an empty {@code
   *     Optional} if no common ancestor exists for the given IDs.
   */
  public Optional<Integer> findLCA(List<Integer> ids) {
    var group = findLCAHelper(root, ids, new HashSet<>(ids));
    if (group.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(group.get().id());
  }

  private Optional<List<Integer>> findPath(int startID, int targetID) {
    // Find the start node first
    var startNode = findGroupByID(root, startID);
    if (startNode.isEmpty()) {
      return Optional.empty(); // Start node not found
    }
    List<Integer> path = new ArrayList<>();
    if (dfs(startNode.get(), targetID, path)) {
      return Optional.of(path); // Path found
    }
    return Optional.empty(); // Path not found
  }

  // Helper method to perform DFS
  private boolean dfs(Group current, int targetID, List<Integer> path) {
    path.add(current.id());
    if (current.id() == targetID) {
      return true; // Target found
    }
    for (Group child : current.children()) {
      if (dfs(child, targetID, path)) {
        return true; // Target found in subtree
      }
    }
    path.remove(path.size() - 1); // Backtrack
    return false; // Target not found in this path
  }

  // Helper method to find a group by its ID
  private Optional<Group> findGroupByID(Group current, int id) {
    if (current.id() == id) {
      return Optional.of(current);
    }
    for (Group child : current.children()) {
      var found = findGroupByID(child, id);
      if (found.isPresent()) {
        return found;
      }
    }
    return Optional.empty(); // Not found
  }

  // Helper method to find LCA
  private Optional<Group> findLCAHelper(Group current, List<Integer> ids, Set<Integer> idSet) {
    if (current == null) {
      return Optional.empty();
    }
  
    int lcaCount = 0;
    Optional<Group> tempLCA = Optional.empty();
  
    for (Group child : current.children()) {
      var lca = findLCAHelper(child, ids, idSet);
      if (lca.isPresent()) {
        lcaCount++;
        tempLCA = lca;
      }
    }
  
    if (idSet.contains(current.id())) {
      return Optional.of(current); // Current node is part of the LCA
    }
  
    if (lcaCount > 1) {
      return Optional.of(current); // Current node is the LCA as it connects more than one path
    }
  
    return tempLCA; // Return the single LCA found among children, if any
  }
}
