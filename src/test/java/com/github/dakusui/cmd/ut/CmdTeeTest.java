package com.github.dakusui.cmd.ut;

import com.github.dakusui.cmd.Cmd;
import com.github.dakusui.cmd.Shell;
import com.github.dakusui.cmd.core.StreamableProcess;
import com.github.dakusui.cmd.utils.TestUtils;
import org.junit.Test;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import static com.github.dakusui.cmd.utils.TestUtils.allOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class CmdTeeTest {
  @Test
  public void consumeTest() throws InterruptedException {
    List<TestUtils.Item<String>> out = Collections.synchronizedList(new LinkedList<>());
    Stream<String> in = Stream.of("a", "b", "c");
    boolean result = Cmd.local(
        "cat -n"
    ).configure(
        new StreamableProcess.Config.Builder(in).build()
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

  @Test
  public void teeTest() throws InterruptedException {
    List<TestUtils.Item<String>> out = Collections.synchronizedList(new LinkedList<>());
    boolean result = Cmd.cmd(
        Shell.local(),
        "seq 1 10000"
    ).tee(
    ).connect(
        in -> Cmd.cmd(
            Shell.local(),
            "cat -n",
            in
        ).stream(
        ),
        s -> out.add(TestUtils.item("LEFT", s))
    ).connect(
        in -> Cmd.cmd(
            Shell.local(),
            "cat -n",
            in
        ).stream(),
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
                .check(">5,000", v -> v > 5_000)
                .build()
        ));
    assertTrue(result);
  }

  private TestUtils.MatcherBuilder<List<TestUtils.Item<String>>, Integer> outMatcherBuilder() {
    return TestUtils.matcherBuilder();
  }
}
