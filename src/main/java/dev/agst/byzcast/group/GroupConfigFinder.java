package dev.agst.byzcast.group;

import java.nio.file.Paths;

/**
 * Provides functionality to locate the configuration directory for a specific ByzCast group based
 * on its ID. The class uses a base path as a reference point to derive the paths to individual
 * group configuration directories.
 */
public class GroupConfigFinder {
  private final String basePath;

  /**
   * Constructs a GroupConfigFinder with a specified base path. This base path is used as the root
   * directory from which the configuration directories for individual groups are derived.
   *
   * @param basePath The base path where group configuration directories are located.
   */
  public GroupConfigFinder(String basePath) {
    this.basePath = basePath;
  }

  /**
   * Returns the configuration home directory path for the specified group ID.
   *
   * @param groupID the group ID
   * @return the configuration home directory path
   */
  public String forGroup(int groupID) {
    String groupSuffix;
    if (groupID < 10) {
      groupSuffix = "g0" + groupID;
    } else {
      groupSuffix = "g" + groupID;
    }

    return Paths.get(basePath, groupSuffix).toString();
  }
}
