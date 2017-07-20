package com.github.dakusui.cmd.ut;

import com.github.dakusui.cmd.Shell;
import com.github.dakusui.cmd.core.IoUtils;
import com.github.dakusui.cmd.core.StreamableProcess;
import com.github.dakusui.cmd.utils.TestUtils;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.stream.Stream;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class StreamableProcessTest extends TestUtils.TestBase {
  private static final Logger LOGGER = LoggerFactory.getLogger(StreamableProcessTest.class);

  @Test(timeout = 3_000)
  public void givenEcho$whenRunLocally$thenMessagePrinted() {
    new StreamableProcess(
        localShell(),
        "echo hello && echo world",
        config(
            Stream.empty()
        )
    ).stream(
    ).forEach(
        System.err::println
    );
  }

  @Test(timeout = 20_000)
  public void givenStreamPipedToCat$whenRunLocally$thenMessagePrinted() {
    new StreamableProcess(
        localShell(),
        "cat",
        config(
            TestUtils.list("data", 10).stream()
        )
    ).stream(
    ).forEach(
        System.err::println
    );
  }

  @Test(timeout = 20_000)
  public void givenLargeDataPipedToCat$whenRunLocally$thenMessagePrinted() {
    new StreamableProcess(
        localShell(),
        "cat",
        config(
            TestUtils.list("data", 100_000).stream()
        )
    ).stream(
    ).forEach(
        System.err::println
    );
  }

  @Test(timeout = 40_000)
  public void givenVeryLargeDataPipedToCat$whenRunLocally$thenMessagePrinted() {
    new StreamableProcess(
        localShell(),
        "cat",
        config(
            TestUtils.list("data", 1_000_000).stream()
        )
    ).stream(
    ).forEach(
        System.err::println
    );
  }

  @Test(timeout = 20_000)
  public void givenDataPipedToCat$whenRunLocallyWithCascaded$thenMessagePrinted() {
    new StreamableProcess(
        localShell(),
        "cat",
        config(
            new StreamableProcess(
                localShell(),
                "cat",
                config(
                    TestUtils.list("data", 2_000).stream().peek(
                        LOGGER::info
                    )
                )
            ).stream().peek(
                LOGGER::info
            )
        )
    ).stream(
    ).forEach(
        LOGGER::info
    );
  }

  @Test(timeout = 20_000)
  public void givenLargeDataPipedToCat$whenRunLocallyWithCascaded$thenMessagePrinted() {
    new StreamableProcess(
        localShell(),
        "cat",
        config(
            new StreamableProcess(
                localShell(),
                "cat",
                config(
                    TestUtils.list("data", 20_000).stream().peek(
                        LOGGER::info
                    )
                )
            ).stream().peek(
                LOGGER::info
            )
        )
    ).stream(
    ).forEach(
        LOGGER::info
    );
  }


  private Shell localShell() {
    return new Shell.Builder.ForLocal().build();
  }

  private StreamableProcess.Config config(Stream<String> stdin) {
    return new StreamableProcess.Config.Builder(
    ).charset(
        Charset.defaultCharset()
    ).configureStdin(
        stdin.peek(LOGGER::trace)
    ).configureStdout(
        IoUtils.nop(),
        s -> s
    ).configureStderr(
        IoUtils.nop(),
        s -> s
    ).build();
  }
}
