package dev.agst.byzcast.groups;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Represents a configuration of groups and their connections.
 */
public class GroupsConfig {
    private final Map<Integer, Set<Integer>> adjacencyList;

    /**
     * Constructs a new GroupsConfig object.
     */
    public GroupsConfig() {
        this.adjacencyList = new HashMap<>();
    }

    /**
     * Adds a connection between two groups.
     *
     * @param group1 the first group
     * @param group2 the second group
     */
    public void addConnection(int group1, int group2) {
        adjacencyList.computeIfAbsent(group1, k -> new HashSet<>()).add(group2);
        adjacencyList.computeIfAbsent(group2, k -> new HashSet<>()).add(group1);
    }

    /**
     * Returns the next group to traverse to reach the target group from the
     * starting group.
     *
     * @param start  the starting group
     * @param target the target group
     * @return the next group to traverse, or null if there is no path from start to
     *         target
     */
    public Integer getNextGroup(int start, int target) {
        if (!adjacencyList.containsKey(start) || !adjacencyList.containsKey(target)) {
            return null; // Return null if start or target group doesn't exist
        }

        Queue<Integer> queue = new LinkedList<>();
        Map<Integer, Integer> predecessors = new HashMap<>();
        Set<Integer> visited = new HashSet<>();

        queue.add(start);
        visited.add(start);

        // Breadth-first search
        while (!queue.isEmpty()) {
            int current = queue.poll();
            if (current == target) {
                break;
            }

            for (int neighbor : adjacencyList.get(current)) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    predecessors.put(neighbor, current);
                    queue.add(neighbor);
                }
            }
        }

        Integer current = target;
        Integer nextGroup = null;
        while (predecessors.containsKey(current)) {
            nextGroup = current;
            current = predecessors.get(current);
            if (current.equals(start)) {
                return nextGroup;
            }
        }

        return null;
    }

    /**
     * Returns all associated groups for a given group.
     *
     * @param group the group
     * @return a set of associated groups
     */
    public Set<Integer> getAssociatedGroups(int group) {
        return adjacencyList.getOrDefault(group, new HashSet<>());
    }
}