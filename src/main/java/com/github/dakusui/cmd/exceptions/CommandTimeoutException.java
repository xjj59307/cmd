package com.github.dakusui.cmd.exceptions;

public class CommandTimeoutException extends CommandException {
  /**
   * Serial version UIT.x
   */
  private static final long serialVersionUID = 6202136371314715671L;

  public CommandTimeoutException(String message, Throwable t) {
    super(message, t);
  }
}
