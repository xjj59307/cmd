package com.github.dakusui.streamablecmd.exceptions;

import java.util.function.Predicate;

/**
 * Thrown when an assumption 'streamable-cmd' by design makes is broken. In other
 * words, if an instance of this class is thrown, it suggests that a bug in the
 * product is detected.
 */
public class StreamableCmdException extends BaseException {
  protected StreamableCmdException(String message) {
    super(message);
  }

  public static <T> T check(T object, Predicate<T> checker) {
    return (T) Exceptions.check(object, checker, Exceptions.factory(StreamableCmdException.class));
  }
}
