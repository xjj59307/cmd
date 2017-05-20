package com.github.dakusui.cmd;

import com.github.dakusui.cmd.core.StreamableProcess;
import com.github.dakusui.cmd.exceptions.UnexpectedExitValueException;
import com.github.dakusui.cmd.exceptions.Exceptions;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
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

  int exitValue();

  void destroy();

  int getPid();

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
          config);
    }

    public Builder addAll(List<String> command) {
      command.forEach(this::add);
      return this;
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

    private final String[]          shell;
    private final Io                io;
    private final String            command;
    private       StreamableProcess process;

    Impl(String[] shell, String command, Io io) {
      this.shell = shell;
      this.command = command;
      this.io = io;
    }

    @Override
    public synchronized Stream<String> run() {
      ExecutorService excutorService = Executors.newFixedThreadPool(3);
      process = startProcess(excutorService);
      return Stream.concat(
          process.getSelector().select(),
          Stream.of(SENTINEL)
      ).filter(
          o -> {
            if (o == SENTINEL) {
              try {
                try {
                  try {
                    int exitValue = waitFor(process);
                    if (!(this.io.exitValueChecker().test(exitValue))) {
                      throw new UnexpectedExitValueException(
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
    public synchronized int exitValue() {
      if (this.process == null)
        throw new IllegalStateException();
      return this.process.exitValue();
    }

    @Override
    public synchronized void destroy() {
      if (this.process == null)
        throw new IllegalStateException();
      this.process.destroy();
    }

    @Override
    public synchronized int getPid() {
      if (this.process == null)
        throw new IllegalStateException();
      return this.process.getPid();
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

    private StreamableProcess startProcess(ExecutorService executorService) {
      return new StreamableProcess(
          createProcess(this.shell, this.command),
          executorService,
          this.io,
          SUPPRESS
      );
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

    Charset charset();

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

      private Stream<String>   stdin;
      private Consumer<String> stdoutConsumer;
      private Consumer<String> stderrConsumer;
      private boolean          redirectsStdout;
      private boolean          redirectsStderr;
      private IntPredicate     exitValueChecker;
      private Charset          charset;

      public Builder(Stream<String> stdin) {
        this.configureStdin(stdin);
        this.charset(Charset.defaultCharset());
        this.checkExitValueWith(value -> value == 0);
        this.configureStdout(NOP, true);
        this.configureStderr(NOP, false);
      }

      public Builder configureStdin(Stream<String> stdin) {
        this.stdin = Objects.requireNonNull(stdin);
        return this;
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
        this.exitValueChecker = Objects.requireNonNull(exitValueChecker);
        return this;
      }

      public Builder charset(Charset charset) {
        this.charset = Objects.requireNonNull(charset);
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

          @Override
          public Charset charset() {
            return Builder.this.charset;
          }
        };
      }
    }
  }
}
