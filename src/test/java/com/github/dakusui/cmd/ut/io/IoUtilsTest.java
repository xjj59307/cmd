package com.github.dakusui.cmd.ut.io;

import com.github.dakusui.cmd.core.IoUtils;
import com.github.dakusui.cmd.utils.TestUtils;
import org.junit.Test;

import java.util.stream.Stream;

public class IoUtilsTest extends TestUtils.TestBase {
  @Test(timeout = 10_000)
  public void givenSimpleConsumer$whenDoFlowControl$thenNotBlocked() {
    Stream.of("hello", "world").forEach(
        IoUtils.flowControlValve(
            System.out::println,
            1
        )
    );
  }
}
