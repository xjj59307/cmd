package com.github.dakusui.cmd;

import com.github.dakusui.cmd.core.Selector;
import com.github.dakusui.cmd.core.StreamableProcess;
import com.github.dakusui.cmd.exceptions.CommandExecutionException;
import com.github.dakusui.cmd.exceptions.Exceptions;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

public interface Cmd {
  Stream<String> run();

  List<String> getShell();

  static Stream<String> run(Shell shell, String... commandLine) {
    return run(shell, new Cmd.Io.Builder(Stream.empty()).build(), commandLine);
  }

  static Stream<String> run(Shell shell, Io io, String... commandLine) {
    return new Cmd.Builder() {{
      Arrays.stream(commandLine).forEach(this::add);
    }}
        .withShell(shell)
        .configure(io)
        .build()
        .run();
  }

  class Builder {
    Shell shell;
    List<String> command = new LinkedList<>();
    Charset      charset = Charset.defaultCharset();
    Io           config  = Io.create();


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
          this.shell.composeCommandLine(),
          String.join(" ", this.command),
          charset,
          config);
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

    private final String[] shell;
    private final Charset  charset;
    private final Io       io;
    private final String   command;

    Impl(String[] shell, String command, Charset charset, Io io) {
      this.shell = shell;
      this.command = command;
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
                    int exitValue = waitFor(process);
                    if (!(this.io.exitValueChecker().test(exitValue))) {
                      throw new CommandExecutionException(
                          exitValue,
                          this.toString(),
                          process.getPid()
                      );
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
    public List<String> getShell() {
      return asList(shell);
    }

    @Override
    public String toString() {
      return String.format("%s '%s'", String.join(" ", this.shell), this.command);
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
          createProcess(this.shell, this.command),
          charset);
    }

    private static Process createProcess(String[] shell, String command) {
      try {
        return Runtime.getRuntime().exec(
            Stream.concat(
                Arrays.stream(shell),
                Stream.of(command)
            ).collect(toList()).toArray(new String[shell.length + 1])
        );
      } catch (IOException e) {
        throw Exceptions.wrap(e);
      }
    }
  }

  interface Io {
    Stream<String> stdin();

    Consumer<String> stdoutConsumer();

    Consumer<String> stderrConsumer();

    boolean redirectsStdout();

    boolean redirectsStderr();

    IntPredicate exitValueChecker();

    static Io create() {
      return builder().build();
    }

    static Io.Builder builder() {
      return builder(Stream.empty());
    }

    static Io.Builder builder(Stream<String> stdin) {
      return new Cmd.Io.Builder(stdin);
    }


    class Builder {
      private static final Consumer<String> NOP = s -> {
      };

      private Stream<String> stdin;
      private Consumer<String> stdoutConsumer   = NOP;
      private Consumer<String> stderrConsumer   = NOP;
      private boolean          redirectsStdout  = true;
      private boolean          redirectsStderr  = false;
      private IntPredicate     exitValueChecker = value -> value == 0;


      public Builder(Stream<String> stdin) {
        this.stdin = Objects.requireNonNull(stdin);
      }

      public Builder configureStdout(Consumer<String> consumer) {
        return this.configureStdout(consumer, this.redirectsStdout);
      }

      public Builder configureStdout(Consumer<String> consumer, boolean redirect) {
        this.stdoutConsumer = Objects.requireNonNull(consumer);
        this.redirectsStdout = redirect;
        return this;
      }

      public Builder configureStderr(Consumer<String> consumer) {
        return this.configureStderr(consumer, this.redirectsStderr);
      }

      public Builder configureStderr(Consumer<String> consumer, boolean redirect) {
        this.stderrConsumer = Objects.requireNonNull(consumer);
        this.redirectsStderr = redirect;
        return this;
      }

      public Builder checkExitValueWith(IntPredicate exitValueChecker) {
        this.exitValueChecker = exitValueChecker;
        return this;
      }

      public Io build() {
        return new Io() {
          @Override
          public Stream<String> stdin() {
            return Stream.concat(
                Builder.this.stdin,
                Stream.of((String) null)
            );
          }

          @Override
          public Consumer<String> stdoutConsumer() {
            return Builder.this.stdoutConsumer;
          }

          @Override
          public Consumer<String> stderrConsumer() {
            return Builder.this.stderrConsumer;
          }

          @Override
          public boolean redirectsStdout() {
            return Builder.this.redirectsStdout;
          }

          @Override
          public boolean redirectsStderr() {
            return Builder.this.redirectsStderr;
          }

          @Override
          public IntPredicate exitValueChecker() {
            return Builder.this.exitValueChecker;
          }
        };
      }
    }
  }
}
