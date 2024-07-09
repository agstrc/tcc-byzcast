package dev.agst.byzcast.groups;

import java.nio.file.Paths;

/**
 * The ConfigHomeFinder class is responsible for finding the configuration home directory for a
 * given group ID.
 */
public class GroupConfigHomeFinder {
  private final String basePath;

  /**
   * Constructs a ConfigHomeFinder object with the specified base path.
   *
   * @param basePath the base path for the configuration home directory
   */
  public GroupConfigHomeFinder(String basePath) {
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
