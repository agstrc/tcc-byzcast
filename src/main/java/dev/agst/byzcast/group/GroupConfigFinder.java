package dev.agst.byzcast.group;

import java.nio.file.Path;

/**
 * Manages the retrieval of group-specific configuration directory paths.
 *
 * <p>This class facilitates locating the configuration directory for a specific group. It operates
 * within a predefined base directory, which serves as the root for all group configuration
 * directories.
 */
public class GroupConfigFinder {
  String configsHome;

  /**
   * Initializes a new instance of {@code GroupConfigFinder} with a given base directory.
   *
   * @param configsHome The base directory path where group configuration directories are located.
   */
  public GroupConfigFinder(String configsHome) {
    this.configsHome = configsHome;
  }

  /**
   * Retrieves the path to a specific group's configuration directory.
   *
   * @param groupID The unique identifier of the group whose configuration directory path is sought.
   * @return The path to the specified group's configuration directory.
   */
  public String forGroup(int groupID) {
    var groupDir = String.format("g%02d", groupID);
    return Path.of(configsHome, groupDir).toString();
  }
}
