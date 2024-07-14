package dev.agst.byzcast.topology;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

class TopologyLoader {
  public static Map<Integer, List<Integer>> loadFrom(String path) throws TopologyLoadException {
    var adjacencyList = new TreeMap<Integer, List<Integer>>();
    var gson = new Gson();
    var groupListType = new TypeToken<List<Group>>() {}.getType();

    try (FileReader reader = new FileReader(path)) {
      List<Group> groups = gson.fromJson(reader, groupListType);

      // we must make sure to first populate the keys, in case there are groups which are
      // only declared through the "associatedGroups" field and not as a "groupID" field
      groups.forEach(
          group -> {
            var associatedGroups = group.associatedGroups();
            associatedGroups.forEach(
                (ag) -> {
                  adjacencyList.putIfAbsent(group.groupID(), new ArrayList<>());
                });
          });

      groups.forEach(
          group -> {
            adjacencyList.put(group.groupID(), group.associatedGroups());
          });
      return adjacencyList;
    } catch (Exception e) {
      throw new TopologyLoadException(e);
    }
  }

  private static record Group(Integer groupID, List<Integer> associatedGroups) {}
}
