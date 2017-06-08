package com.github.dakusui.cmd.utils;

import com.github.dakusui.cmd.Cmd;
import com.github.dakusui.cmd.Shell;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.DiagnosingMatcher;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;

public enum TestUtils {
  ;

  static final PrintStream STDOUT = System.out;
  static final PrintStream STDERR = System.err;

  public static <T, U> MatcherBuilder<T, U> matcherBuilder() {
    return MatcherBuilder.create();
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
    String         functionName  = "transform";
    Function<V, U> f             = null;

    public MatcherBuilder<V, U> transform(String name, Function<V, U> f) {
      this.functionName = Objects.requireNonNull(name);
      this.f = Objects.requireNonNull(f);
      return this;
    }

    public MatcherBuilder<V, U> check(String name, Predicate<U> p) {
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
          .transform("passthrough", t -> t);
    }

    public static <T, U> MatcherBuilder<T, U> create() {
      return new MatcherBuilder<>();
    }
  }

  /**
   * A bit better version of CoreMatchers.allOf.
   * For example:
   * <pre>assertThat("myValue", allOf(startsWith("my"), containsString("Val")))</pre>
   */
  @SafeVarargs
  public static <T> Matcher<T> allOf(Matcher<? super T>... matchers) {
    return new DiagnosingMatcher<T>() {
      @Override
      protected boolean matches(Object o, Description mismatch) {
        boolean ret = true;
        for (Matcher<? super T> matcher : matchers) {
          if (!matcher.matches(o)) {
            if (ret)
              mismatch.appendText("(");
            mismatch.appendText("\n  ");
            mismatch.appendDescriptionOf(matcher).appendText(" ");
            matcher.describeMismatch(o, mismatch);
            ret = false;
          }
        }
        if (!ret)
          mismatch.appendText("\n)");
        return ret;
      }

      @Override
      public void describeTo(Description description) {
        description.appendList("(\n  ", " " + "and" + "\n  ", "\n)", Arrays.stream(matchers).collect(toList()));
      }
    };
  }

  public static class Item<D> {
    public final String symbol;
    public final D      value;

    Item(String symbol, D value) {
      this.symbol = Objects.requireNonNull(symbol);
      this.value = value;
    }

    public String toString() {
      return String.format("(%s:%s)", symbol, value);
    }
  }

  public static <D> Item<D> item(String symbol, D value) {
    return new Item<>(symbol, value);
  }

  public static <D> int countInterleaves(List<Item<D>> listOfD) {
    AtomicReference<String> previousSymbol = new AtomicReference<>(null);
    AtomicInteger count = new AtomicInteger(0);

    Objects.requireNonNull(listOfD).forEach(dItem -> {
      if (previousSymbol.get() != null && !Objects.equals(dItem.symbol, previousSymbol.get()))
        count.getAndIncrement();
      previousSymbol.set(dItem.symbol);
    });

    return count.get();
  }

  public static class SelfTest {
    @Test
    public void testCountInterLeaves() {
      assertEquals(2, countInterleaves(asList(item("A", 1), item("A", 1), item("B", 1), item("A", 2))));
    }
  }
}
