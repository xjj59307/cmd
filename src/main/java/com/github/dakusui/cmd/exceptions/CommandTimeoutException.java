package com.github.dakusui.cmd.exceptions;

public class CommandTimeoutException extends CommandException {
  public CommandTimeoutException(Throwable t) {
    super(t);
  }

  public CommandTimeoutException(String message) {
    super(message, null);
  }
}
