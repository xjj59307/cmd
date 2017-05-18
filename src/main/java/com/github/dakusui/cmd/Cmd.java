package com.github.dakusui.cmd;

import com.github.dakusui.cmd.core.Selector;
import com.github.dakusui.cmd.core.StreamableProcess;
import com.github.dakusui.cmd.exceptions.CommandExecutionException;
import com.github.dakusui.cmd.exceptions.Exceptions;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.Arrays.asList;

public interface Cmd {
  Stream<String> run();

  List<String> getCommandLine();

  static Stream<String> run(Shell shell, String... commandLine) {
    return run(shell, Io.DEFAULT, commandLine);
  }

  static Stream<String> run(Shell shell, Io io, String... commandLine) {
    return new Cmd.Builder() {{
      Arrays.stream(commandLine).forEach(this::add);
    }}.withShell(shell).configure(io).build().run();
  }


  class Builder {
    Shell shell;
    List<String> command = new LinkedList<>();
    Charset      charset = Charset.defaultCharset();
    Io           config  = Io.DEFAULT;


    public Builder add(String arg) {
      this.command.add(arg);
      return this;
    }

    public Builder withShell(Shell shell) {
      this.shell = shell;
      return this;
    }

    public Builder configure(Io config) {
      this.config = config;
      return this;
    }

    public Cmd build() {
      return new Impl(
          composeCommandLine(),
          charset,
          config);
    }

    private String[] composeCommandLine() {
      return this.shell.buildCommandLine(String.join(" ", this.command));
    }
  }

  class Impl implements Cmd {
    private static final Predicate<Object> SUPPRESS = new Predicate<Object>() {
      @Override
      public boolean test(Object o) {
        return o == SENTINEL;
      }
    };
    private static final Object            SENTINEL = new Object();

    private final String[] commandLine;
    private final Charset  charset;
    private final Io       io;

    Impl(String[] commandLine, Charset charset, Io io) {
      this.commandLine = commandLine;
      this.charset = charset;
      this.io = io;
    }

    @Override
    public Stream<String> run() {
      StreamableProcess process = startProcess();
      ExecutorService excutorService = Executors.newFixedThreadPool(3);
      return Stream.concat(
          new Selector.Builder<String>()
              .add(this.io.stdin(), process.stdin())
              .add(
                  process.stdout()
                      .map(s -> {
                        this.io.stdoutConsumer().accept(s);
                        return s;
                      })
                      .filter(SUPPRESS.or(s -> io.redirectsStdout()))
              )
              .add(
                  process.stderr()
                      .map(s -> {
                        this.io.stderrConsumer().accept(s);
                        return s;
                      })
                      .filter(SUPPRESS.or(s -> io.redirectsStderr()))
              )
              .withExecutorService(excutorService)
              .build()
              .select(),
          Stream.of(SENTINEL)
      ).filter(
          o -> {
            if (o == SENTINEL) {
              try {
                try {
                  try {
                    try {
                      int exitValue = waitFor(process);
                      if (!(this.io.exitValue().test(exitValue))) {
                        throw new CommandExecutionException(exitValue, this.getCommandLine().toArray(new String[0]), process.getPid());
                      }
                      ////
                      // A sentinel shouldn't be passed to following stages.
                      return false;
                    } finally {
                      process.stdout().close();
                    }
                  } finally {
                    process.stderr().close();
                  }
                } finally {
                  process.stdin().accept(null);
                }
              } finally {
                excutorService.shutdown();
              }
            }
            return true;
          }
      ).map(
          o -> (String) o
      );
    }

    @Override
    public List<String> getCommandLine() {
      return asList(commandLine);
    }

    private int waitFor(Process process) {
      try {
        return process.waitFor();
      } catch (InterruptedException e) {
        throw Exceptions.wrap(e);
      }
    }

    private StreamableProcess startProcess() {
      return new StreamableProcess(
          createProcess(this.commandLine),
          charset);
    }

    private Process createProcess(String[] commandLine) {
      try {
        return Runtime.getRuntime().exec(commandLine);
      } catch (IOException e) {
        throw Exceptions.wrap(e);
      }
    }
  }

  interface Io {
    Io DEFAULT = new Impl(Stream.empty());

    Stream<String> stdin();

    Consumer<String> stdoutConsumer();

    Consumer<String> stderrConsumer();

    boolean redirectsStdout();

    boolean redirectsStderr();

    IntPredicate exitValue();

    abstract class Base implements Io {
      private final Stream<String> stdin;
      private Consumer<String> stdoutConsumer    = this::consumeStdout;
      private Consumer<String> stderrConsumer    = this::consumeStderr;
      private IntPredicate     exitValueConsumer = this::exitValue;

      protected Base(Stream<String> stdin) {
        this.stdin = Stream.concat(
            Exceptions.Arguments.requireNonNull(stdin),
            Stream.of((String) null)
        );
      }

      @Override
      public Stream<String> stdin() {
        return this.stdin;
      }

      @Override
      public Consumer<String> stdoutConsumer() {
        return stdoutConsumer;
      }

      @Override
      public Consumer<String> stderrConsumer() {
        return stderrConsumer;
      }

      @Override
      public IntPredicate exitValue() {
        return this.exitValueConsumer;
      }

      abstract protected void consumeStdout(String s);

      abstract protected void consumeStderr(String s);

      abstract protected boolean exitValue(int exitValue);
    }

    class Impl extends Base {
      public Impl(Stream<String> stdin) {
        super(stdin);
      }

      @Override
      protected void consumeStdout(String s) {
      }

      @Override
      protected void consumeStderr(String s) {
      }

      @Override
      protected boolean exitValue(int exitValue) {
        return exitValue == 0;
      }

      @Override
      public boolean redirectsStdout() {
        return true;
      }

      @Override
      public boolean redirectsStderr() {
        return false;
      }
    }
  }

}
