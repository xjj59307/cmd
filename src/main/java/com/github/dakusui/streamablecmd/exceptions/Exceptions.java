package com.github.dakusui.streamablecmd.exceptions;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Function;
import java.util.function.Predicate;

public enum Exceptions {
  ;
  static <T, E extends RuntimeException> T check(T object, Predicate<T> checker, Function<String, E> exceptionFactory) {
    return check(object, v -> v, checker, exceptionFactory);
  }

  static <T, U, E extends RuntimeException> T check(T object, Function<T, U> transformer, Predicate<U> checker, Function<String, E> exceptionFactory) {
    if (checker.test(transformer.apply(object)))
      return object;
    throw exceptionFactory.apply(String.format("'%s=%s(%s)' did not satisfy '%s'", transformer.apply(object), transformer, object, checker));
  }

  static <E extends BaseException> Function<String, E> factory(Class<E> cmdExceptionClass) {
    return s -> {
      try {
        //noinspection unchecked
        return (E) cmdExceptionClass.getConstructor(String.class).newInstance(s);
      } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
        throw new RuntimeException(e);
      }
    };
  }

  static <T, U> Function<T, U> transformer(String stringForm, Function<T, U> transformer) {
    return new Function<T, U>() {
      @Override
      public U apply(T t) {
        return transformer.apply(t);
      }

      @Override
      public String toString() {
        return stringForm;
      }
    };
  }

  static <U> Predicate<U> checker(String stringForm, Predicate<U> checker) {
    return new Predicate<U>() {
      @Override
      public boolean test(U u) {
        return checker.test(u);
      }

      @Override
      public String toString() {
        return stringForm;
      }
    };
  }
}
