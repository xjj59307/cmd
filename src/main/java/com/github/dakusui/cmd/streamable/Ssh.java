package com.github.dakusui.cmd.streamable;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.function.Consumer;
import java.util.function.IntPredicate;
import java.util.stream.Stream;

public interface Ssh {

  Stream<String> run();

  class Impl implements Ssh {
    private static final Object sentinel = new Object();

    private final IntPredicate exitCodeChecker;
    private final String       commandLine;
    private final Charset      charset;

    private Impl(String commandLine, IntPredicate exitCodeChecker, Charset charset) {
      this.exitCodeChecker = exitCodeChecker;
      this.commandLine = commandLine;
      this.charset = charset;
    }

    @Override
    public Stream<String> run() {
      StreamableProcess process = new StreamableProcess(
          createProcess(this.commandLine),
          s -> {
          },
          s -> {
          },
          charset);

      return Stream.concat(
          process.stdout(),
          Stream.of(sentinel)
      ).filter(o -> {
        try {
          if (o == sentinel) {
            int exitCode = process.waitFor();
            if (!(exitCodeChecker.test(exitCode))) {
              throw new SshException(exitCode, commandLine, process.stderr());
            }
            return false;
          }
          return true;
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }).map(o -> (String) o);
    }

    private Process createProcess(String commandLine) {
      try {
        return Runtime.getRuntime().exec(commandLine);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  class Builder {
    String           commandLine    = null;
    String           hostName       = "localhost";
    String           userName       = null;
    Charset          charset        = Charset.defaultCharset();
    Consumer<String> stderrConsumer = new Consumer<String>() {
      @Override
      public void accept(String s) {
        // does nothing by default
      }
    };
    private IntPredicate exitCodeChecker = value -> value == 0;

    public Ssh build() {
      return new Impl(commandLine, exitCodeChecker, charset);
    }
  }
}
