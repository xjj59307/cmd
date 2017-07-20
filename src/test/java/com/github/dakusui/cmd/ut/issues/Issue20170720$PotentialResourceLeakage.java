package com.github.dakusui.cmd.ut.issues;

import com.github.dakusui.cmd.StreamableQueue;
import com.github.dakusui.cmd.core.IoUtils;
import com.github.dakusui.cmd.core.Selector;
import com.github.dakusui.cmd.ut.StreamableProcessTest;
import com.github.dakusui.cmd.utils.TestUtils;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.stream.Stream;

/**
 * This is a test class to reproduce a nasty 'issue' I hit, which 'large data' handling
 * ({@code StreamableProcessTest#givenLargeDataPipedToCat$whenRunLocally$thenMessagePrinted}) times out
 * after the test 't01_select100Kdata3' is executed.
 *
 * It is speculated that this is caused by some resource leakage in 't01_select100Kdata3',
 * which uses {@code Selector} in an un-intended way (2 downstreams connected through
 * {@code StreamableQueue}s.)
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class Issue20170720$PotentialResourceLeakage {
  @Test
  public void t01_select100Kdata3() {
    StreamableQueue<String> down1 = new StreamableQueue<>(100);
    StreamableQueue<String> down2 = new StreamableQueue<>(100);
    Stream<String> up = TestUtils.list("stdin", 100_000).stream().parallel().peek(down1).peek(down2);
    new Thread(() -> {
      up.forEach(IoUtils.nop());
      Stream.of(down1, down2).parallel().forEach(each -> each.accept(null));
    }).start();
    new Selector.Builder<String>(
        "UT"
    ).add(
        TestUtils.<String>list("stdout", 100_000).stream(),
        IoUtils.nop(),
        true
    ).add(
        TestUtils.<String>list("stderr", 100_000).stream(),
        System.err::println,
        false
    ).build(
    ).stream().forEach(
        System.out::println
    );
  }

  @Test(timeout = 20_000)
  public void t02_givenLargeDataPipedToCat$whenRunLocally$thenMessagePrinted() {
    new StreamableProcessTest().givenLargeDataPipedToCat$whenRunLocally$thenMessagePrinted();
  }
}
