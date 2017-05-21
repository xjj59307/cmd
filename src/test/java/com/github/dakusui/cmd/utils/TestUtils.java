package com.github.dakusui.cmd.utils;

import com.github.dakusui.cmd.Cmd;
import com.github.dakusui.cmd.Shell;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;

import java.io.*;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public enum TestUtils {
  ;

  static final PrintStream STDOUT = System.out;
  static final PrintStream STDERR = System.err;

  private static <T> Matcher<T> matcher(Predicate<T> check) {
    return new BaseMatcher<T>() {
      @Override
      public boolean matches(Object item) {
        return check.test((T) item);
      }

      @Override
      public void describeTo(Description description) {

      }
    };
  }

  public static boolean sleepAndReturn(boolean value) {
    try {
      Thread.sleep(1);
    } catch (InterruptedException ignored) {
    }
    return value;
  }

  /**
   * A base class for tests which writes to stdout/stderr.
   */
  public static class TestBase {
    @Before
    public void suppressStdOutErrIfRunUnderSurefire() {
      TestUtils.suppressStdOutErrIfRunUnderSurefire();
    }

    @After
    public void restoreStdOutErr() {
      TestUtils.restoreStdOutErr();
    }
  }


  public static void suppressStdOutErrIfRunUnderSurefire() {
    if (TestUtils.isRunUnderSurefire()) {
      System.setOut(new PrintStream(new OutputStream() {
        @Override
        public void write(int b) throws IOException {
        }
      }));
      System.setErr(new PrintStream(new OutputStream() {
        @Override
        public void write(int b) throws IOException {
        }
      }));
    }
  }

  public static void restoreStdOutErr() {
    System.setOut(STDOUT);
    System.setOut(STDERR);
  }

  public static boolean isRunUnderSurefire() {
    return System.getProperty("surefire.real.class.path") != null;
  }

  public static String identity() {
    String key = "commandstreamer.identity";
    if (!System.getProperties().containsKey(key))
      return String.format("%s/.ssh/id_rsa", Cmd.stream(Shell.local(), "echo $HOME").collect(Collectors.joining()));
    return System.getProperty(key);
  }

  public static String userName() {
    String key = "commandstreamer.username";
    if (!System.getProperties().contains(key))
      return System.getProperty("user.name");
    return System.getProperty(key);
  }

  public static String hostName() {
    ///
    // Safest way to get hostname. (or least bad way to get it)
    // See http://stackoverflow.com/questions/7348711/recommended-way-to-get-hostname-in-java
    return Cmd.stream(Shell.local(), "hostname").collect(Collectors.joining());
  }

  public static InputStream openForRead(File file) {
    try {
      return new FileInputStream(file);
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public static OutputStream openForWrite(File file) {
    try {
      return new FileOutputStream(file);
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  private static File createTempFile() {
    try {
      File ret = File.createTempFile("commandrunner-streamable-", ".log");
      ret.deleteOnExit();
      return ret;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static class MatcherBuilder<V, U> {
    String         predicateName = "P";
    Predicate<U>   p             = null;
    String         functionName  = "f";
    Function<V, U> f             = null;

    public MatcherBuilder<V, U> f(String name, Function<V, U> f) {
      this.functionName = Objects.requireNonNull(name);
      this.f = Objects.requireNonNull(f);
      return this;
    }

    public MatcherBuilder<V, U> p(String name, Predicate<U> p) {
      this.predicateName = Objects.requireNonNull(name);
      this.p = Objects.requireNonNull(p);
      return this;
    }

    public Matcher<V> build() {
      Objects.requireNonNull(p);
      Objects.requireNonNull(f);
      return new BaseMatcher<V>() {
        @SuppressWarnings("unchecked")
        @Override
        public boolean matches(Object item) {
          return p.test(f.apply((V) item));
        }

        @SuppressWarnings("unchecked")
        @Override
        public void describeMismatch(Object item, Description description) {
          description
              .appendText("was false because " + functionName + "(x)=")
              .appendValue(f.apply((V) item))
              .appendText("; x=")
              .appendValue(item)
          ;
        }

        @Override
        public void describeTo(Description description) {
          description.appendText(String.format("%s(%s(x))", predicateName, functionName));
        }
      };
    }

    public static <T> MatcherBuilder<T, T> simple() {
      return new MatcherBuilder<T, T>()
          .f("passthrough", t -> t);
    }

    public static <T, U> MatcherBuilder<T, U> matcherBuilder() {
      return new MatcherBuilder<>();
    }
  }
}
