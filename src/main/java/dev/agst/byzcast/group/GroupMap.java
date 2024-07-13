package dev.agst.byzcast.group;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;

/**
 * This class represents a mapping of groups to their associated groups, forming a graph structure.
 * It provides functionality to load this mapping from a JSON source and to find a step in the
 * shortest path between two groups using a breadth-first search algorithm.
 */
public class GroupMap {
  /**
   * A map representing the adjacency list of the graph. Each key is a group ID, and its value is a
   * list of IDs of directly associated groups.
   */
  private final Map<Integer, List<Integer>> adjacencyList;

  /**
   * Constructs a GroupMap instance with a pre-defined adjacency list.
   *
   * @param adjacencyList A map where each key is a group ID and its value is a list of IDs of
   *     directly associated groups.
   */
  public GroupMap(Map<Integer, List<Integer>> adjacencyList) {
    this.adjacencyList = adjacencyList;
  }

  /**
   * Constructs a GroupMap instance by loading the adjacency list from a JSON file. The file format
   * must match the following example structure:
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
   */
  public GroupMap(String filePath) throws GroupMapLoadException {
    var adjacencyList = GroupMap.loadGroupsConfig(filePath);
    this.adjacencyList = adjacencyList;
  }

  /**
   * Loads the groups configuration from the specified file path and returns a map representing the
   * adjacency list.
   *
   * @param filePath the path to the file containing the groups configuration
   * @return a map representing the adjacency list of the groups
   * @throws GroupMapLoadException if an error occurs while loading the groups configuration
   */
  private static Map<Integer, List<Integer>> loadGroupsConfig(String filePath)
      throws GroupMapLoadException {
    HashMap<Integer, List<Integer>> adjacencyList = new HashMap<>();

    Gson gson = new Gson();
    Type groupListType = new TypeToken<List<Group>>() {}.getType();

    try (FileReader reader = new FileReader(filePath)) {
      List<Group> groups = gson.fromJson(reader, groupListType);
      groups.forEach(group -> adjacencyList.put(group.groupID, group.associatedGroups));
      return adjacencyList;
    } catch (IOException | JsonIOException | JsonSyntaxException e) {
      throw new GroupMapLoadException(e);
    }
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

  private static record Group(Integer groupID, List<Integer> associatedGroups) {}
}
