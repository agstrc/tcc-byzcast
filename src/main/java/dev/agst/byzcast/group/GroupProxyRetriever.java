package dev.agst.byzcast.group;

import bftsmart.tom.ServiceProxy;
import java.util.HashMap;
import java.util.Random;

/**
 * The GroupProxyRetriever class is responsible for retrieving service proxies for groups. It
 * maintains a mapping of group IDs to service proxies and provides a method to retrieve the service
 * proxy for a given group ID.
 */
public class GroupProxyRetriever {
  private HashMap<Integer, ServiceProxy> proxies = new HashMap<>();
  private Random random = new Random();

  private GroupConfigFinder configFinder;

  /**
   * Constructs a GroupProxyRetriever object with the specified GroupConfigFinder.
   *
   * @param configFinder The GroupConfigFinder used to find the configuration for groups.
   */
  public GroupProxyRetriever(GroupConfigFinder configFinder) {
    this.configFinder = configFinder;
  }

  /**
   * Retrieves the service proxy for the specified group ID. If a service proxy for the group ID
   * does not exist, a new one is created and added to the mapping.
   *
   * @param groupID The ID of the group.
   * @return The service proxy for the specified group ID.
   */
  public ServiceProxy forGroup(int groupID) {
    return proxies.computeIfAbsent(
        groupID,
        (key) -> {
          var configHome = configFinder.forGroup(groupID);
          var clientID = random.nextInt(Integer.MAX_VALUE);
          return new ServiceProxy(clientID, configHome);
        });
  }
}
