package com.github.dakusui.cmd.exceptions;

public class CommandInterruptionException extends CommandExecutionException {
  public CommandInterruptionException() {
    super(null, null);
  }
}
