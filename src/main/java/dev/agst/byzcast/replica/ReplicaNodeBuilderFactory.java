package dev.agst.byzcast.replica;

import dev.agst.byzcast.Logger;
import dev.agst.byzcast.group.GroupConfigFinder;
import dev.agst.byzcast.group.GroupProxies;
import dev.agst.byzcast.topology.TopologyManager;

/**
 * A builder class for creating instances of {@link ReplicaNode}. This class facilitates the
 * step-by-step construction of {@code ReplicaNode} instances, ensuring that all necessary
 * components are properly initialized before the instance is created.
 *
 * @see ReplicaNode
 */
public class ReplicaNodeBuilderFactory {

  // the following interfaces are separated between steps. Through the use of these interfaces we
  // can, at compile time, ensure that all necessary components are properly initialized before the
  // instance is built.

  public static interface LoggerConfigurator {
    InfoConfigurator withLogger(Logger logger);
  }

  public static interface InfoConfigurator {
    ConfigFinderConfigurartor withInfo(ReplicaInfo info);
  }

  public static interface ConfigFinderConfigurartor {
    TopologyManagerConfigurator withConfigFinder(GroupConfigFinder configFinder);
  }

  public static interface TopologyManagerConfigurator {
    RequestCountConfigurator withTopologyManager(TopologyManager topologyManager);
  }

  public static interface RequestCountConfigurator {
    Buildable withTargetRequestCount(int targetRequestCount);
  }

  public static interface Buildable {
    ReplicaNode build();
  }

  static interface ReplicaNodeBuilderSteps
      extends LoggerConfigurator,
          InfoConfigurator,
          ConfigFinderConfigurartor,
          TopologyManagerConfigurator,
          RequestCountConfigurator,
          Buildable {}

  /**
   * The {@code Builder} class implements the step-by-step construction process for {@link
   * ReplicaNode} instances. It adheres to the builder pattern, ensuring that a {@code ReplicaNode}
   * is only constructed once all its necessary components have been properly set. This class
   * implements the {@link ReplicaNodeBuilderSteps} interface, which defines the sequence of
   * configuration steps to be followed.
   *
   * <p>Usage example:
   *
   * <pre>
   * ReplicaNode replicaNode = new ReplicaNodeBuilderFactory.Builder()
   *     .withLogger(new Logger())
   *     .withInfo(new ReplicaInfo())
   *     .withConfigFinder(new GroupConfigFinder())
   *     .withTopologyManager(new TopologyManager(topology))
   *     .withTargetRequestCount(3)
   *     .build();
   * </pre>
   */
  public static class Builder implements ReplicaNodeBuilderSteps {
    private Logger logger;
    private ReplicaInfo info;
    private GroupProxies proxies;
    private TopologyManager topologyManager;
    private int targetRequestCount;

    @Override
    public InfoConfigurator withLogger(Logger logger) {
      this.logger = logger;
      return this;
    }

    @Override
    public ConfigFinderConfigurartor withInfo(ReplicaInfo info) {
      this.info = info;
      return this;
    }

    @Override
    public TopologyManagerConfigurator withConfigFinder(GroupConfigFinder configFinder) {
      this.proxies = new GroupProxies(configFinder);
      return this;
    }

    @Override
    public RequestCountConfigurator withTopologyManager(TopologyManager topologyManager) {
      this.topologyManager = topologyManager;
      return this;
    }

    @Override
    public Buildable withTargetRequestCount(int targetRequestCount) {
      this.targetRequestCount = targetRequestCount;
      return this;
    }

    @Override
    public ReplicaNode build() {
      var requestHandler = new RequestHandler(logger, info, proxies, topologyManager);
      var state = new ReplicaState(targetRequestCount);
      var replicaNode = new ReplicaNode(logger, requestHandler, state, topologyManager);

      return replicaNode;
    }
  }
}
