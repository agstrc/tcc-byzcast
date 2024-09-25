package dev.agst.byzcast.client;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

class StatsWriter {
  // since this writer is compatible with the previous implementation, we still require the type
  // field but for our use cases, it'll always be "global"
  static final String type = "global";

  public static void writeStats(Writer writer, List<Stat> stats) throws IOException {
    writer.write("ORDER\tLATENCY\tABS\tTYPE\tUUIDn");

    if (stats.size() == 0) return;

    var startTime = stats.get(0).beforeRequest();
    for (int i = 0; i < stats.size(); i++) {
      var stat = stats.get(i);
      var latency = stat.afterRequest() - stat.beforeRequest();
      var absolute = stat.afterRequest() - startTime;

      var line =
          String.format("%d\t%d\t%d\t%s\t%s\n", i + 1, latency, absolute, "global", stat.id());
      writer.write(line);
    }
  }
}
