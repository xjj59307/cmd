package com.github.dakusui.cmd.exceptions;

/**
 * An exception class to indicate an error is detected during command execution
 *
 * @author ukaihiroshi01
 */
public abstract class CommandException extends RuntimeException {
  /**
   * Creates an object of this class.
   *
   * @param msg A message string to be set.
   * @param t   A nested exception
   */
  CommandException(String msg, Throwable t) {
    super(msg, t);
  }

  CommandException(Throwable throwable) {
    super(throwable);
  }
}
