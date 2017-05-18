package com.github.dakusui.cmd.exceptions;

public class InternalException extends CommandException {
  InternalException(Throwable throwable) {
    super(throwable);
  }

  InternalException(String message) {
    super(message, null);
  }
}
