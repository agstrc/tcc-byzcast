package dev.agst.byzcast.group;

import bftsmart.tom.ServiceProxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Provides a mechanism for managing and accessing {@link ServiceProxy} instances for different
 * groups.
 */
public class GroupProxies {
  private final GroupConfigFinder configFinder;
  private final Map<Integer, ServiceProxy> proxies = new HashMap<>();

  private final Random random = new Random();

  /**
   * Constructs a {@code GroupProxies} instance with the specified {@link GroupConfigFinder}.
   *
   * @param configFinder The {@link GroupConfigFinder} used to locate configuration directories for
   *     groups.
   */
  public GroupProxies(GroupConfigFinder configFinder) {
    this.configFinder = configFinder;
  }

  /**
   * Retrieves or creates a {@link ServiceProxy} for the specified group ID.
   *
   * <p>If a {@link ServiceProxy} for the given group ID already exists, it is returned. Otherwise,
   * a new {@link ServiceProxy} instance is created using a randomly generated client ID and the
   * group-specific configuration directory path obtained from the {@link GroupConfigFinder}. The
   * newly created {@link ServiceProxy} is then stored and returned.
   *
   * @param groupID The ID of the group for which to retrieve or create a {@link ServiceProxy}.
   * @return The {@link ServiceProxy} associated with the specified group ID.
   */
  public ServiceProxy forGroup(int groupID) {
    return this.proxies.computeIfAbsent(
        groupID,
        (id) -> {
          var clientID = this.random.nextInt(Integer.MAX_VALUE);
          var config = this.configFinder.forGroup(groupID);
          return new ServiceProxy(clientID, config);
        });
  }
}
