package com.github.dakusui.cmd.compat.exceptions;

import com.github.dakusui.cmd.exceptions.CommandException;

public class CommandTimeoutException extends CommandException {
  public CommandTimeoutException(Throwable t) {
    super(t);
  }
}
