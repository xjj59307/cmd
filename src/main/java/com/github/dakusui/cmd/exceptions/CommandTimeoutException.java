package com.github.dakusui.cmd.exceptions;

@Deprecated
public class CommandTimeoutException extends CommandException {
  public CommandTimeoutException(Throwable t) {
    super(t);
  }
}
