package dev.agst.byzcast.groups;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.agst.byzcast.exceptions.InvalidConfigException;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * The GroupsConfigLoader class is responsible for loading group configurations
 * from a JSON file.
 */
public class GroupsConfigLoader {
    private final ObjectMapper objectMapper;

    /**
     * Represents a group with an ID and a list of associated groups.
     *
     * @param groupID          the ID of the group
     * @param associatedGroups the list of associated groups
     */
    private record Group(Integer groupID, List<Integer> associatedGroups) {
    }

    public GroupsConfigLoader() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Loads a group configuration from a JSON file. The expected format of the JSON
     * file is an array of objects, where each object represents a group with a
     * "groupID" and an "associatedGroups" array. For example:
     * 
     * <pre>
     * [
     *   {
     *     "groupID": 1,
     *     "associatedGroups": [2, 3]
     *   },
     *   {
     *     "groupID": 2,
     *     "associatedGroups": [1]
     *   }
     * ]
     * </pre>
     *
     * @param filePath the path to the JSON file
     * @return the loaded GroupsConfig object
     * @throws IOException            if an I/O error occurs while reading the file
     * @throws InvalidConfigException if the configuration file is invalid
     */
    public GroupsConfig loadFromJson(String filePath) throws IOException, InvalidConfigException {
        List<Group> groups;
        try {
            groups = objectMapper.readValue(new File(filePath), new TypeReference<List<Group>>() {
            });
        } catch (DatabindException e) {
            throw new InvalidConfigException("Configuration file " + filePath + " is invalid: " + e.toString());
        }

        var groupsConfig = new GroupsConfig();
        for (var group : groups) {
            for (var associatedGroup : group.associatedGroups()) {
                groupsConfig.addConnection(group.groupID(), associatedGroup);
            }
        }

        return groupsConfig;
    }
}