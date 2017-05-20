package com.github.dakusui.cmd.exceptions;

public class CommandExecutionException extends CommandException {
  CommandExecutionException(String msg, Throwable t) {
    super(msg, t);
  }
}
