package com.github.dakusui.cmd.tmp;

import com.github.dakusui.cmd.core.StreamableProcess;
import com.github.dakusui.cmd.utils.TestUtils;
import org.junit.Test;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import static com.github.dakusui.cmd.tmp.CmdTeeTest.outMatcherBuilder;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class TeeSandbox {
  @Test(timeout = 3_000)
  public void teeExample() throws InterruptedException {
    List<TestUtils.Item<String>> out = Collections.synchronizedList(new LinkedList<>());
    Stream<String> in = Stream.of("a", "b", "c");
    boolean result = CompatCmd.local(
        "cat -n"
    ).configure(StreamableProcess.Config.builder().configureStdin(in).build()).build(
    ).tee(
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

  @Test(timeout = 3_000)
  public void connectExample() throws InterruptedException {
    List<TestUtils.Item<String>> out = Collections.synchronizedList(new LinkedList<>());
    Stream<String> in = Stream.of("a", "b", "c");
    CompatCmd.local(
        "cat -n"
    ).configure(StreamableProcess.Config.builder().configureStdin(in).build()).build(
    ).stream(
    ).forEach(
        s -> out.add(TestUtils.item("DOWNSTREAM", s))
    );
    assertThat(
        out,
        outMatcherBuilder()
            .transform("size", List::size)
            .check("==3", v -> v == 3)
            .build()
    );
  }
}
