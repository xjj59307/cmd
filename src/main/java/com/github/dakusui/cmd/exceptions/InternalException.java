package com.github.dakusui.cmd.exceptions;

class InternalException extends CommandException {
  InternalException(String message, Throwable throwable) {
    super(message, throwable);
  }

  InternalException(Throwable throwable) {
    super(throwable);
  }
}
