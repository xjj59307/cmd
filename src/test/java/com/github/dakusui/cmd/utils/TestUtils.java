package com.github.dakusui.cmd.utils;

import com.github.dakusui.cmd.Cmd;
import com.github.dakusui.cmd.Shell;
import com.github.dakusui.cmd.exceptions.Exceptions;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.DiagnosingMatcher;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.util.ArrayList;
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

  public static <T, U> MatcherBuilder<T, U> matcherBuilder(String name, Function<T, U> transformer) {
    return MatcherBuilder.<T, U>create().<T, U>transform(name, transformer);
  }

  public static String base64() {
    String systemName = systemName();
    String ret;
    if ("Linux".equals(systemName)) {
      ret = "base64 -w";
    } else if ("Mac OS X".equals(systemName)) {
      ret = "base64 -b";
    } else {
      throw new RuntimeException(String.format("%s is not a supported platform.", systemName));
    }
    return ret;
  }

  public static String systemName() {
    return System.getProperty("os.name");
  }

  public static List<String> list(String prefix, int size) {
    List<String> ret = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      ret.add(String.format("%s-%s", prefix, i));
    }
    return ret;
  }

  /**
   * A base class for tests which writes to to/stderr.
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
      return String.format("%s/.ssh/id_rsa", Cmd.cmd("echo $HOME").stream().collect(Collectors.joining()));
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
    return Cmd.cmd(Shell.local(), "hostname").stream().collect(Collectors.joining());
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
          try {
            if (!matcher.matches(o)) {
              if (ret)
                mismatch.appendText("(");
              mismatch.appendText("\n  ");
              mismatch.appendDescriptionOf(matcher).appendText(" ");
              matcher.describeMismatch(o, mismatch);
              ret = false;
            }
          } catch (Exception e) {
            if (ret)
              mismatch.appendText("(");
            mismatch.appendText("\n  ");
            mismatch
                .appendDescriptionOf(matcher)
                .appendText(" on ")
                .appendValue(o)
                .appendText(" ")
                .appendText(String.format("failed with %s(%s)", e.getClass().getCanonicalName(), e.getMessage()));
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

  public static boolean terminatesIn(Runnable runnable, long millis) {
    Thread t = new Thread(runnable);
    t.start();
    try {
      long before = System.currentTimeMillis();
      while (true) {
        if (System.currentTimeMillis() - before >= millis) {
          boolean ret = Thread.State.TERMINATED.equals(t.getState());
          if (!ret)
            Arrays.stream(t.getStackTrace()).forEach(e -> System.out.println("\t" + e));
          return ret;
        }
        if (Thread.State.TERMINATED.equals(t.getState()))
          return true;
        Thread.sleep(1);
      }
    } catch (InterruptedException e) {
      throw Exceptions.wrap(e);
    }
  }

  public static class SelfTest {
    @Test
    public void testCountInterLeaves() {
      assertEquals(2, countInterleaves(asList(item("A", 1), item("A", 1), item("B", 1), item("A", 2))));
    }
  }
}
