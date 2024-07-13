package dev.agst.byzcast;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Represents a configuration-free logger, with key-value pairs as attributes for log messages. The
 * logger supports INFO and ERROR logging levels, and outputs to the standard output stream.
 */
public class Logger {
  /**
   * Stores custom attributes for log messages. The usage of a TreeMap guarantees that the
   * attributes are sorted by key.
   */
  private Map<String, String> attributes = new TreeMap<>();

  /** Constructs a new Logger instance without any initial attributes. */
  public Logger() {}

  /**
   * Creates a new Logger instance with an additional attribute.
   *
   * @param key The key of the attribute to add.
   * @param value The value of the attribute.
   * @return A new Logger instance with the added attribute.
   */
  public Logger with(String key, String value) {
    var child = new Logger();
    child.attributes.putAll(this.attributes);
    child.attributes.put(key, value);
    return child;
  }

  /**
   * Creates a new Logger instance with an additional attribute.
   *
   * @param key The key of the attribute to add.
   * @param value The value of the attribute. Its toString() method will be called.
   * @return A new Logger instance with the added attribute.
   */
  public <T extends Object> Logger with(String key, T value) {
    return with(key, value.toString());
  }

  /**
   * Logs an error message with the current attributes.
   *
   * @param message The error message to log.
   */
  public void error(String message) {
    this.logMessage("ERROR", message);
  }

  /**
   * Logs an error message with the current attributes and an exception.
   *
   * @param message The error message to log.
   * @param e The exception to log.
   */
  public void error(String message, Exception e) {
    var logger = this.with("exception", e.getMessage());
    logger.error(message);
  }

  /**
   * Logs an informational message with the current attributes.
   *
   * @param message The informational message to log.
   */
  public void info(String message) {
    this.logMessage("INFO", message);
  }

  /**
   * Logs a message with a specified level and the current attributes.
   *
   * @param level The level of the log message (e.g., "INFO", "ERROR").
   * @param message The message to log.
   */
  private void logMessage(String level, String message) {
    StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();

    String callerFullyQualifiedName = "Unknown";
    if (stackTraceElements.length > 3) {
      StackTraceElement caller = stackTraceElements[3];
      callerFullyQualifiedName =
          String.format(
              "%s.%s:%s", caller.getClassName(), caller.getMethodName(), caller.getLineNumber());
    }

    String attributesString =
        attributes.entrySet().stream()
            .map(entry -> String.format("%s=%s", entry.getKey(), entry.getValue()))
            .collect(Collectors.joining(" "));

    System.out.println(
        String.format("%s %s %s %s", callerFullyQualifiedName, level, attributesString, message));
  }
}
