package dev.agst.byzcast.client;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * A utility class for writing statistics to a {@link Writer} in a format compatible with the
 * previous ByzCast implementation.
 *
 * <p>This class maintains compatibility with the previous ByzCast implementation to facilitate the
 * use of existing analysis scripts for the previous implementation available at <a
 * href="https://github.com/jefnvo/byzantine-fault-tolerant-atomic-multicast">Byzantine Fault
 * Tolerant Atomic Multicast</a>. The statistics are written in a tab-separated format with columns
 * for order, latency, absolute time, and type.
 */
class StatsWriter {
  // since this writer is compatible with the previous implementation, we still require the type
  // field but for our use cases, it'll always be "global"
  static final String type = "global";

  public static void writeStats(Writer writer, List<Stat> stats) throws IOException {
    writer.write("ORDER\tLATENCY\tABS\tTYPE\n");

    var startTime = stats.get(0).beforeRequest();
    for (int i = 0; i < stats.size(); i++) {
      var stat = stats.get(i);
      var latency = stat.afterRequest() - stat.beforeRequest();
      var absolute = stat.afterRequest() - startTime;

      var line = String.format("%d\t%d\t%d\t%s\n", i + 1, latency, absolute, "global");
      writer.write(line);
    }
  }
}
