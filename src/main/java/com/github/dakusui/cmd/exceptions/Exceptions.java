package com.github.dakusui.cmd.exceptions;

import java.util.function.Function;

public enum Exceptions {
  ;

  public static <T extends RuntimeException> T wrap(Throwable throwable) {
    throw wrap(throwable, InternalException::new);
  }

  public static <T extends RuntimeException> T wrap(Throwable throwable, Function<Throwable, T> exceptionFactory) {
    if (throwable instanceof Error)
      throw (Error) throwable;
    if (throwable instanceof RuntimeException)
      throw (RuntimeException) throwable;
    throw exceptionFactory.apply(throwable);
  }

  public static IllegalStateException illegalState(Object currentState, String requirement) {
    throw new IllegalStateException(String.format("Current state=<%s>, while it should be <%s>", currentState, requirement));
  }

  public static InternalException illegalException(Throwable t) {
    throw new InternalException("This exception shouldn't be thrown here", t);
  }
}
