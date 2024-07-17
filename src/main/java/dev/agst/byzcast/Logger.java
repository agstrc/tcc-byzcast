package dev.agst.byzcast;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

// I didn't really like how needlessly complex the setups for most of Java's logging libraries
// were.

/**
 * A simple logging class that supports logging messages with different levels (INFO, ERROR) and
 * attributes. It allows chaining of attributes using the {@code with} method for contextual
 * logging.
 */
public class Logger {
  /** The attributes of the logger. The usage of a LinkedHashMap guarantees attribute ordering */
  private LinkedHashMap<String, String> attributes;

  /**
   * Constructs a new logger instance with the specified attributes.
   *
   * @param attributes A TreeMap containing initial attributes for the logger.
   */
  private Logger(LinkedHashMap<String, String> attributes) {
    this.attributes = attributes;
  }

  /** Constructs a new logger instance with no initial attributes. */
  public Logger() {
    this(new LinkedHashMap<>());
  }

  /**
   * Logs a message at the INFO level.
   *
   * @param message The message to be logged.
   */
  public void info(String message) {
    logMessage("INFO", message);
  }

  /**
   * Logs a message at the ERROR level.
   *
   * @param message The message to be logged.
   */
  public void error(String message) {
    logMessage("ERROR", message);
  }

  /**
   * Logs a message at the ERROR level along with an exception.
   *
   * @param message The message to be logged.
   * @param e The exception associated with the error.
   */
  public void error(String message, Throwable e) {
    var stackTrace =
        Arrays.stream(e.getStackTrace())
            .map(StackTraceElement::toString)
            .map(String::trim)
            .collect(Collectors.joining(", "));

    var logger = this.with("exception", e.toString()).with("stackTrace", stackTrace);
    logger.error(message);
  }

  /**
   * Adds an attribute to the logger and returns a new logger instance with this attribute. This
   * method allows for chaining of attributes.
   *
   * @param key The key of the attribute.
   * @param value The value of the attribute.
   * @return A new {@code NewLogger} instance with the added attribute.
   */
  public Logger with(String key, Object value) {
    var attributes = new LinkedHashMap<>(this.attributes);
    attributes.put(key, value.toString());

    return new Logger(attributes);
  }

  /**
   * Logs a message with the specified level and the current attributes of the logger.
   *
   * @param level The level of the log message (e.g., INFO, ERROR).
   * @param message The message to be logged.
   */
  private void logMessage(String level, String message) {
    var now = LocalDateTime.now();
    var formatter = DateTimeFormatter.ISO_DATE_TIME;
    var nowString = now.format(formatter);

    var stackTrace = Thread.currentThread().getStackTrace();
    var caller = "unknown:0";

    for (int i = 1; i < stackTrace.length; i++) {
      var element = stackTrace[i];
      var callerClass = element.getClassName();

      if (!callerClass.equals(Logger.class.getName())) {
        caller = element.getClassName() + ":" + element.getLineNumber();
        break;
      }
    }

    var attributesString =
        this.attributes.entrySet().stream()
            .map(entry -> String.format("%s=%s", entry.getKey(), entry.getValue()))
            .collect(Collectors.joining(" "));

    String log;
    if (attributes.size() > 0) {
      log = String.format("%s %s %s %s %s", nowString, caller, level, attributesString, message);
    } else {
      log = String.format("%s %s %s %s", nowString, caller, level, message);
    }
    System.out.println(log);
  }
}
