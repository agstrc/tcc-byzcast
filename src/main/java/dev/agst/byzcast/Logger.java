package dev.agst.byzcast;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// I didn't really like how needlessly complex the setups for most of Java's logging libraries
// were.

/**
 * A simple logging class that supports logging messages with different levels (INFO, ERROR) and
 * attributes. It allows chaining of attributes using the {@code with} method for contextual
 * logging.
 */
public class Logger {
  /**
   * Represents a simple key-value pair attribute. This record is used to store additional
   * information that can be attached to log messages, allowing for more detailed and contextual
   * logging. The key is a {@code String} that identifies the attribute, and the value is an {@code
   * Object} that represents the attribute's value. The object's {@code toString} method is called
   * to get the string representation of the value.
   */
  public static record Attr(String key, Object value) {}

  /** The inherent attributes of the current logger. */
  private final List<Attr> attrs;

  /** Whether the logger is enabled or not. */
  private boolean enabled = true;

  /** Constructs a new logger instance with no initial attributes. */
  public Logger() {
    this.attrs = new ArrayList<>();
  }

  /**
   * Constructs a new logger instance with the specified attributes.
   *
   * @param attrs The attributes to be attached to the logger.
   * @param enabled Whether the logger is enabled or not.
   */
  private Logger(List<Attr> attrs, boolean enabled) {
    this.attrs = attrs;
    this.enabled = enabled;
  }

  /**
   * Disables the logger, preventing it from logging any messages.
   *
   * @return The current logger instance.
   */
  public Logger disable() {
    this.enabled = false;
    return this;
  }

  /**
   * Enables the logger, allowing it to log messages.
   *
   * @return The current logger instance.
   */
  public Logger enable() {
    this.enabled = true;
    return this;
  }

  /**
   * Creates a new logger instance with the specified attributes appended to the current logger's
   * attributes.
   *
   * @param attrs The attributes to be appended to the logger.
   * @return A new logger instance with the specified attributes.
   */
  public Logger with(Attr... attrs) {
    var childAttrs = new ArrayList<>(this.attrs);
    for (var attr : attrs) {
      childAttrs.add(attr);
    }

    return new Logger(childAttrs, this.enabled);
  }

  /**
   * Logs a message at the INFO level.
   *
   * @param message The message to be logged.
   * @param attrs The attributes to be attached to the log message.
   */
  public void info(String message, Attr... attrs) {
    logMessage("INFO", message, attrs);
  }

  /**
   * Logs a message at the ERROR level.
   *
   * @param message The message to be logged.
   * @param attrs The attributes to be attached to the log message.
   */
  public void error(String message, Attr... attrs) {
    logMessage("ERROR", message, attrs);
  }

  /**
   * Logs a message at the ERROR level along with an exception.
   *
   * @param message The message to be logged.
   * @param e The exception associated with the error.
   * @param attrs The attributes to be attached to the log message.
   */
  public void error(String message, Throwable e, Attr... attrs) {
    var exceptionAttr = new Attr("exception", e.toString());

    var stackTraceMessage =
        Arrays.stream(e.getStackTrace())
            .map(StackTraceElement::toString)
            .map(String::trim)
            .collect(Collectors.joining(", "));
    var stackTraceAttr = new Attr("stackTrace", stackTraceMessage);

    var throwableAttrs = new Attr[] {exceptionAttr, stackTraceAttr};
    var finalAttrs =
        Stream.concat(Arrays.stream(attrs), Arrays.stream(throwableAttrs)).toArray(Attr[]::new);

    logMessage("ERROR", message, finalAttrs);
  }

  private void logMessage(String level, String message, Attr... attrs) {
    if (!this.enabled) {
      return;
    }

    var now = LocalDateTime.now();
    var nowString = now.format(DateTimeFormatter.ISO_DATE_TIME);

    var finalAttributes =
        Stream.concat(this.attrs.stream(), Arrays.stream(attrs))
            .map(
                attr -> {
                  var key = attr.key();
                  var value = attr.value().toString();
                  return key + "=" + value;
                })
            .collect(Collectors.joining(" "));

    String log;
    if (finalAttributes.isEmpty()) {
      log = String.format("%s %s %s", nowString, level, message);
    } else {
      log = String.format("%s %s %s %s", nowString, level, finalAttributes, message);
    }
    System.out.println(log);
  }
}
