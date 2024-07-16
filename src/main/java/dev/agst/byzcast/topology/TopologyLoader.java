package dev.agst.byzcast.topology;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * The {@code TopologyLoader} class is responsible for loading the network topology from a JSON file
 * and constructing a {@code Group} hierarchy that represents the network structure. The JSON file
 * should contain a list of lists, where each list represents a group and its children.
 *
 * <p>The class provides a static method {@code loadFromJSON} that reads the topology from a given
 * file path and constructs the {@code Group} hierarchy accordingly. It ensures that the topology is
 * a valid tree structure and throws an exception if any group attempts to have an ancestor as its
 * child, preventing circular dependencies.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * Group rootGroup = TopologyLoader.loadFromJSON("path/to/topology.json");
 * }</pre>
 *
 * @see Group
 */
class TopologyLoader {
  /**
   * Loads the network topology from a JSON file and constructs a {@code Group} hierarchy.
   *
   * <p>This method reads a JSON file specified by the {@code path} parameter, expecting a list of
   * lists format. Each list corresponds to a group, identified by the index of that list, and
   * contains the group IDs of its children. This structure is used to construct a hierarchy of
   * {@code Group} objects representing the network topology.
   *
   * <p>Example of expected JSON format:
   *
   * <pre>{@code
   * [
   *   [1, 2], // Group 0 has children 1 and 2
   *   [3],    // Group 1 has child 3
   *   [],     // Group 2 has no children
   *   []      // Group 3 has no children
   * ]
   * }</pre>
   *
   * This represents a topology where group 0 is the root, having two children (groups 1 and 2), and
   * group 1 further has one child (group 3).
   *
   * <p>If the JSON structure implies a circular dependency, where a group attempts to have an
   * ancestor as its child, the method throws an exception to prevent invalid topology
   * configurations.
   *
   * @param path The file path to the JSON file containing the topology data.
   * @return The root {@code Group} of the constructed hierarchy.
   * @throws Exception If there is an error reading the file, parsing the JSON, or if the JSON
   *     structure implies a circular dependency.
   */
  public static Group loadFromJSON(String path) throws Exception {
    var gson = new Gson();
    var type = new TypeToken<List<List<Integer>>>() {}.getType();

    try (var reader = new FileReader(path)) {
      List<List<Integer>> groups = gson.fromJson(reader, type);

      var rootGroup = new Group(0, new ArrayList<>());
      var idToGroup = new HashMap<Integer, Group>();
      idToGroup.put(0, rootGroup);

      for (int currentGroupID = 0; currentGroupID < groups.size(); currentGroupID++) {
        var group =
            idToGroup.computeIfAbsent(currentGroupID, (id) -> new Group(id, new ArrayList<>()));

        for (var childID : groups.get(currentGroupID)) {
          if (childID <= currentGroupID) {
            var error =
                String.format(
                    "Group %d attempted to have ancestor %d as child", currentGroupID, childID);
            throw new Exception(error);
          }

          var childGroup =
              idToGroup.computeIfAbsent(childID, (id) -> new Group(id, new ArrayList<>()));
          group.children().add(childGroup);
        }
      }

      return rootGroup;
    }
  }
}
