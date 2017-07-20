package com.github.dakusui.cmd.tmp;

import com.github.dakusui.cmd.Shell;
import com.github.dakusui.cmd.core.StreamableProcess;
import com.github.dakusui.cmd.exceptions.UnexpectedExitValueException;
import com.github.dakusui.cmd.utils.TestUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import static com.github.dakusui.cmd.utils.TestUtils.allOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class CmdTeeTest {
  @Before
  public void before() throws InterruptedException {
    Thread.sleep(500);
  }

  @Test(timeout = 3_000, expected = UnexpectedExitValueException.class)
  public void consumeTestExitingNon0() throws InterruptedException {
    List<TestUtils.Item<String>> out = Collections.synchronizedList(new LinkedList<>());
    Stream<String> in = Stream.of("a", "b", "c");
    boolean result = CompatCmd.local(
        "cat non-existing-file"
    ).configure(
        new StreamableProcess.Config.Builder().configureStdin(in).build()
    ).build().tee(
    ).connect(
        s -> out.add(TestUtils.item("LEFT", s))
    ).connect(
        s -> out.add(TestUtils.item("RIGHT", s))
    ).run();
  }

  @Test(timeout = 3_000)
  public void consumeTest() throws InterruptedException {
    List<TestUtils.Item<String>> out = Collections.synchronizedList(new LinkedList<>());
    Stream<String> in = Stream.of("a", "b", "c");
    boolean result = CompatCmd.local(
        "cat -n"
    ).configure(
        new StreamableProcess.Config.Builder().build()
    ).build().tee(
    ).connect(
        s -> out.add(TestUtils.item("LEFT", s))
    ).connect(
        s -> out.add(TestUtils.item("RIGHT", s))
    ).run();
    assertThat(
        out,
        outMatcherBuilder()
            .transform("size", List::size)
            .check("==6", v -> v == 6)
            .build()
    );

    assertTrue(result);
  }

  @Test(timeout = 3_000, expected = UnexpectedExitValueException.class)
  public void teeExitingWithNon0ConnectedToCommands() throws InterruptedException {
    CompatCmd.cmd(
        Shell.local(),
        "cat not-existing-file"
    ).tee(
    ).connect(
        "cat -n"
    ).connect(
        "cat -n"
    ).run();
  }

  @Test(timeout = 10_000)
  public void teeTest() throws InterruptedException {
    List<TestUtils.Item<String>> out = Collections.synchronizedList(new LinkedList<>());
    try {
      boolean result = CompatCmd.cmd(
          Shell.local(),
          "seq 1 10000"
      ).tee(
      ).connect(
          in -> CompatCmd.cmd(
              Shell.local(),
              "cat -n",
              in
          ),
          s -> out.add(TestUtils.item("LEFT", s))
      ).connect(
          in -> CompatCmd.cmd(
              Shell.local(),
              "cat -n",
              in
          ),
          s -> out.add(TestUtils.item("RIGHT", s))
      ).run();

      assertThat(
          out,
          allOf(
              outMatcherBuilder()
                  .transform("size", List::size)
                  .check("==20,000", v -> v == 20_000)
                  .build(),
            /*
             * Make sure LEFT and RIGHT are executed concurrently.
             */
              outMatcherBuilder()
                  .transform("interleaves", TestUtils::countInterleaves)
                  .check(">3,000", v -> v > 3_000)
                  .build()
          ));
      assertTrue(result);
    } finally {
      System.out.println(out);
    }
  }

  public static TestUtils.MatcherBuilder<List<TestUtils.Item<String>>, Integer> outMatcherBuilder() {
    return TestUtils.matcherBuilder();
  }
}
