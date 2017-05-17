package com.github.dakusui.cmd.exceptions;

/**
 * An exception class to indicate an error is detected during command execution
 *
 * @author ukaihiroshi01
 */
public class CommandException extends RuntimeException {
  /**
   * Creates an object of this class.
   *
   * @param msg A message string to be set.
   * @param t
   */
  public CommandException(String msg, Throwable t) {
    super(msg != null ? msg + "(" + t.getMessage() + ")" : t.getMessage());
    this.setStackTrace(t.getStackTrace());
  }
}
