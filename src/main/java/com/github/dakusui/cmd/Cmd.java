package com.github.dakusui.cmd;

import com.github.dakusui.cmd.core.StreamableProcess;
import com.github.dakusui.cmd.core.Tee;
import com.github.dakusui.cmd.exceptions.CommandInterruptionException;
import com.github.dakusui.cmd.exceptions.Exceptions;
import com.github.dakusui.cmd.exceptions.UnexpectedExitValueException;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

public interface Cmd {
  Stream<String> stream();

  int exitValue();

  void destroy();

  int getPid();

  Shell getShell();

  StreamableProcess.Config getProcessConfig();

  default Cmd connect(String commandLine) {
    return connect(getShell(), commandLine);
  }

  default Cmd connect(Shell shell, String commandLine) {
    return connect(
        shell,
        stdin -> new StreamableProcess.Config.Builder(stdin).build(),
        commandLine
    );
  }

  default Cmd connect(Shell shell, Function<Stream<String>, StreamableProcess.Config> connector, String commandLine) {
    return Cmd.cmd(
        shell,
        connector.apply(this.stream()),
        commandLine
    );
  }

  default Tee.Connector<String> tee() {
    return Tee.tee(this.stream());
  }

  static Stream<String> stream(Shell shell, String commandLine) {
    return cmd(shell, commandLine).stream();
  }

  static Stream<String> stream(Shell shell, StreamableProcess.Config config, String commandLine) {
    return cmd(shell, config, commandLine).stream();
  }

  static Cmd cmd(Shell shell, String commandLine, Stream<String> stdin, Consumer<String> stdout, Consumer<String> stderr) {
    return cmd(
        shell,
        StreamableProcess.Config.builder(
            stdin
        ).configureStdout(
            Objects.requireNonNull(stdout)
        ).configureStderr(
            Objects.requireNonNull(stderr)
        ).build(),
        commandLine
    );
  }

  static Cmd cmd(Shell shell, String commandLine, Stream<String> stdin, Consumer<String> stdout) {
    return cmd(
        shell,
        commandLine,
        stdin,
        stdout,
        s -> {
        }
    );
  }

  static Cmd cmd(Shell shell, String commandLine, Stream<String> stdin) {
    return cmd(
        shell,
        commandLine,
        stdin,
        s -> {
        });
  }

  static Cmd cmd(Shell shell, String commandLine) {
    return cmd(shell, commandLine, Stream.empty());
  }

  static Cmd cmd(Shell shell, StreamableProcess.Config config, String commandLine) {
    return new Cmd.Builder()
        .addAll(Collections.singletonList(commandLine))
        .withShell(shell)
        .configure(config)
        .build();
  }

  static Cmd.Builder local(String... commandLine) {
    return new Cmd.Builder().withShell(Shell.local()).addAll(asList(commandLine));
  }

  class Builder {
    Shell shell;
    List<String>             command = new LinkedList<>();
    StreamableProcess.Config config  = StreamableProcess.Config.create();

    public Builder add(String arg) {
      this.command.add(arg);
      return this;
    }

    public Builder withShell(Shell shell) {
      this.shell = shell;
      return this;
    }

    public Builder configure(StreamableProcess.Config config) {
      this.config = config;
      return this;
    }

    public Cmd build() {
      return new Impl(
          this.shell,
          String.join(" ", this.command),
          config);
    }

    public Builder addAll(List<String> command) {
      command.forEach(this::add);
      return this;
    }
  }

  class Impl implements Cmd {

    enum State {
      NOT_STARTED,
      STARTED,
    }

    static final Object SENTINEL = new Object();

    private final Shell                    shell;
    private final String                   command;
    private       State                    state;
    private       StreamableProcess        process;
    private final StreamableProcess.Config processConfig;
    private       ExecutorService          threadPool;

    Impl(Shell shell, String command, StreamableProcess.Config config) {
      this.shell = shell;
      this.command = command;
      this.processConfig = config;
      this.state = State.NOT_STARTED;
    }

    @Override
    public Stream<String> stream() {
      this.run();
      return Stream.concat(
          this.process.getSelector().select(),
          Stream.of(SENTINEL)
      ).filter(
          o -> {
            if (o == SENTINEL) {
              try {
                try {
                  try {
                    int exitValue = waitFor(process);
                    if (!(this.processConfig.exitValueChecker().test(exitValue))) {
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
                threadPool.shutdown();
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
      if (this.state != State.STARTED)
        throw Exceptions.illegalState(state, "!=State.STARTED");
      return this.process.exitValue();
    }

    @Override
    public synchronized void destroy() {
      if (this.state != State.STARTED)
        throw Exceptions.illegalState(state, "!=State.STARTED");
      this.process.destroy();
    }

    @Override
    public synchronized int getPid() {
      if (this.state == State.NOT_STARTED)
        throw Exceptions.illegalState(this.state, "==State.NOT_STARTED");
      return this.process.getPid();
    }

    @Override
    public Shell getShell() {
      return this.shell;
    }

    @Override
    public StreamableProcess.Config getProcessConfig() {
      return this.processConfig;
    }

    @Override
    public String toString() {
      return String.format("%s '%s'", this.getShell().format(), this.command);
    }

    private synchronized void run() {
      if (state == State.STARTED)
        throw Exceptions.illegalState(state, "!=State.STARTED");
      this.threadPool = Executors.newFixedThreadPool(3);
      this.process = startProcess(this.shell, this.command, this.processConfig, threadPool);
      this.state = State.STARTED;
    }


    private int waitFor(Process process) {
      try {
        return process.waitFor();
      } catch (InterruptedException e) {
        throw Exceptions.wrap(e, (Function<Throwable, RuntimeException>) throwable -> new CommandInterruptionException());
      }
    }

    private static StreamableProcess startProcess(Shell shell, String command, StreamableProcess.Config processConfig, ExecutorService executorService) {
      return new StreamableProcess(
          createProcess(shell, command),
          executorService,
          processConfig
      );
    }

    private static Process createProcess(Shell shell, String command) {
      try {
        return Runtime.getRuntime().exec(
            Stream.concat(
                Stream.concat(
                    Stream.of(shell.program()),
                    shell.options().stream()
                ),
                Stream.of(command)
            ).collect(toList()).toArray(new String[shell.options().size() + 2])
        );
      } catch (IOException e) {
        throw Exceptions.wrap(e);
      }
    }
  }
}
