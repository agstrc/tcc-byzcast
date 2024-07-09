package dev.agst.byzcast.groups;

import bftsmart.tom.ServiceProxy;
import java.util.HashMap;
import java.util.Random;

/** The GroupProxyRetriever class is responsible for retrieving service proxies for groups. */
public class GroupProxyRetriever {
  private final GroupConfigHomeFinder homeFinder;

  Random random = new Random();
  HashMap<Integer, ServiceProxy> proxies = new HashMap<>();

  public GroupProxyRetriever(GroupConfigHomeFinder homeFinder) {
    this.homeFinder = homeFinder;
  }

  /**
   * Retrieves the service proxy for the specified group ID.
   *
   * @param groupID The ID of the group.
   * @return The service proxy for the specified group ID.
   */
  public ServiceProxy getProxyForGroup(int groupID) {
    if (!proxies.containsKey(groupID)) {
      String configPath = homeFinder.forGroup(groupID);
      ServiceProxy proxy = new ServiceProxy(random.nextInt(Integer.MAX_VALUE), configPath);
      proxies.put(groupID, proxy);
    }

    return proxies.get(groupID);
  }
}
