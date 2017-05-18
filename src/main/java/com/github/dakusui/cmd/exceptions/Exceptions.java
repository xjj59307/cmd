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

}
