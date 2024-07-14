package dev.agst.byzcast.topology;

import bftsmart.tom.ServiceProxy;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

/**
 * Represents the topology of a ByzCast network, which is a graph structure of groups and their
 * associated groups. The class provides functionality to find the next group in the adjacency list
 * from a start group to a target group, and to calculate the immediate next steps required to reach
 * each target group from a starting group.
 */
public class Topology {
  /**
   * A map representing the adjacency list of the graph. Each key is a group ID, and its value is a
   * list of IDs of directly associated groups.
   */
  private final Map<Integer, List<Integer>> adjacencyList;

  /** The basepath where the configuration directories for individual groups are located. */
  private final String configsHome;

  /** A map of group IDs to their corresponding service proxies. */
  private final Map<Integer, ServiceProxy> serviceProxies = new TreeMap<>();

  /** A random number generator for generating random numbers. */
  private final Random random = new Random();

  /**
   * Constructs a Topology instance with a pre-defined adjacency list and a base path for group
   * configurations.
   *
   * @param adjacencyList A map where each key is a group ID and its value is a list of IDs of
   *     directly associated groups.
   * @param configsHome The base path where group configuration directories are located.
   */
  public Topology(Map<Integer, List<Integer>> adjacencyList, String configsHome) {
    this.adjacencyList = adjacencyList;
    this.configsHome = configsHome;
  }

  /**
   * Constructs a Topology instance by loading the adjacency list from a JSON file and specifying
   * the base path for group configurations. The file format must match the following example
   * structure:
   *
   * <pre>
   * [
   *    {
   *        "groupID": 1,
   *        "associatedGroups": [2, 3]
   *    },
   *    {
   *        "groupID": 2,
   *        "associatedGroups": [3]
   *    },
   *    {
   *        "groupID": 3,
   *        "associatedGroups": []
   *    }
   * ]
   * </pre>
   *
   * @param topologyConfigPath The path to the JSON file containing the adjacency list.
   * @param configsHomePath The base path where group configuration directories are located.
   * @throws TopologyLoadException If there is an error loading the topology from the file.
   */
  public Topology(String topologyConfigPath, String configsHomePath) throws TopologyLoadException {
    this.adjacencyList = TopologyLoader.loadFrom(topologyConfigPath);
    this.configsHome = configsHomePath;
  }

  /**
   * Retrieves the service proxy for the specified group ID.
   *
   * @param groupID The ID of the group.
   * @return The service proxy for the group.
   * @throws IllegalArgumentException If the group does not exist within the topology.
   */
  public ServiceProxy getServiceProxy(int groupID) {
    if (!adjacencyList.containsKey(groupID)) {
      throw new IllegalArgumentException("Group does not exist within the topology.");
    }

    return serviceProxies.computeIfAbsent(
        groupID,
        (key) -> {
          String configHome = configPathForGroup(groupID);
          return new ServiceProxy(random.nextInt(Integer.MAX_VALUE), configHome);
        });
  }

  /**
   * Returns the configuration home directory path for the specified group ID.
   *
   * @param groupID The group ID.
   * @return The configuration home directory path.
   */
  public String configPathForGroup(int groupID) {
    String groupSuffix;
    if (groupID < 10) {
      groupSuffix = "g0" + groupID;
    } else {
      groupSuffix = "g" + groupID;
    }

    return Path.of(configsHome, groupSuffix).toString();
  }

  /**
   * Finds the next group in the adjacency list from the start group to the target group.
   *
   * @param startGroup The starting group.
   * @param targetGroup The target group.
   * @return An Optional containing the next group towards the target, or an empty Optional if not
   *     found.
   */
  public Optional<Integer> nextGroup(Integer startGroup, Integer targetGroup) {
    if (!adjacencyList.containsKey(startGroup) || !adjacencyList.containsKey(targetGroup)) {
      return Optional.empty(); // Return empty if either start or target group doesn't exist.
    }
    Map<Integer, Integer> parent = new HashMap<>();
    Queue<Integer> queue = new LinkedList<>();
    Set<Integer> visited = new HashSet<>();

    queue.add(startGroup);
    visited.add(startGroup);
    parent.put(startGroup, null); // Start group has no parent.

    while (!queue.isEmpty()) {
      Integer current = queue.poll();
      if (current.equals(targetGroup)) {
        break; // Found the target group.
      }
      for (Integer neighbor : adjacencyList.getOrDefault(current, Collections.emptyList())) {
        if (!visited.contains(neighbor)) {
          queue.add(neighbor);
          visited.add(neighbor);
          parent.put(neighbor, current);
        }
      }
    }

    // Backtrack from target to start to find the path.
    Integer step = targetGroup;
    while (parent.get(step) != null && !parent.get(step).equals(startGroup)) {
      step = parent.get(step);
    }

    return parent.containsKey(step)
        ? Optional.of(step)
        : Optional.empty(); // Return the next step towards the target, or empty if not found.
  }

  /**
   * Calculates the immediate next steps required to reach each target group from a starting group.
   * This method maps each target group to an intermediary group that must be traversed next on the
   * path to the target. Essentially, for every target group, it identifies the next group in the
   * sequence towards reaching that target. This helps in planning a route that covers all specified
   * targets by indicating the next move from the start group.
   *
   * @param startGroup The starting group from which paths to target groups are sought.
   * @param targetGroups A list of target groups for which paths need to be identified.
   * @return An Optional containing a map, where each key is an intermediary group that leads
   *     towards one or more target groups listed in its associated ArrayList. If the start group is
   *     not part of the adjacency list or if any target group is unreachable from the start,
   *     returns an empty Optional.
   */
  public Optional<Map<Integer, List<Integer>>> pathsForTargets(
      Integer startGroup, List<Integer> targetGroups) {
    if (!adjacencyList.containsKey(startGroup)) {
      return Optional.empty(); // Return empty if start group doesn't exist.
    }

    Map<Integer, List<Integer>> nodesToPaths = new TreeMap<>();
    for (var target : targetGroups) {
      if (target == startGroup) {
        return Optional.empty(); // Return empty if target is the same as start group.
      }

      var nextGroup = nextGroup(startGroup, target);
      if (nextGroup.isEmpty()) {
        return Optional.empty();
      }

      var lst = nodesToPaths.computeIfAbsent(nextGroup.get(), key -> new ArrayList<>());
      lst.add(target);
    }

    return Optional.of(nodesToPaths);
  }
}
