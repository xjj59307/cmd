package com.github.dakusui.cmd;

import com.github.dakusui.cmd.core.StreamableProcess;
import com.github.dakusui.cmd.exceptions.Exceptions;
import com.github.dakusui.cmd.exceptions.UnexpectedExitValueException;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

public interface Cmd extends Consumer<String> {
  Stream<String> run();

  int exitValue();

  void destroy();

  int getPid();

  Shell getShell();

  static Stream<String> run(Shell shell, String... commandLine) {
    return cmd(shell, commandLine).run();
  }

  static Stream<String> run(Shell shell, StreamableProcess.Config config, String... commandLine) {
    return cmd(shell, config, commandLine).run();
  }

  static Cmd cmd(Shell shell, String... commandLine) {
    return cmd(shell, StreamableProcess.Config.builder(Stream.empty()).build(), commandLine);
  }

  static Cmd cmd(Shell shell, StreamableProcess.Config config, String... commandLine) {
    return new Cmd.Builder()
        .addAll(asList(commandLine))
        .withShell(shell)
        .configure(config)
        .build();
  }

  static StreamableProcess.Config connect(Stream<String> stdin) {
    return connector(stdin).build();
  }

  static StreamableProcess.Config.Builder connector(Stream<String> stdin) {
    return StreamableProcess.Config.builder(stdin);
  }

  static StreamableProcess.Config.Builder processConfigBuilder(Stream<String> stdin) {
    return new StreamableProcess.Config.Builder(stdin);
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
      RUNNING,
      FINISHED
    }

    static final Object SENTINEL = new Object();

    private final Shell                    shell;
    private final String                   command;
    private       State                    state;
    private       StreamableProcess        process;
    private final StreamableProcess.Config processConfig;

    Impl(Shell shell, String command, StreamableProcess.Config config) {
      this.shell = shell;
      this.command = command;
      this.processConfig = config;
      this.state = State.NOT_STARTED;
    }

    @Override
    public Stream<String> run() {
      ExecutorService excutorService = Executors.newFixedThreadPool(3);
      synchronized (this) {
        this.process = startProcess(this.shell, this.command, this.processConfig, excutorService);
      }
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
    public Shell getShell() {
      return this.shell;
    }

    @Override
    public String toString() {
      return String.format("%s %s '%s'", shell.program(), String.join(" ", shell.options()), this.command);
    }

    private int waitFor(Process process) {
      try {
        return process.waitFor();
      } catch (InterruptedException e) {
        throw Exceptions.wrap(e);
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

    @Override
    public void accept(String s) {
      synchronized (this) {
        while (this.process == null)
          try {
            this.wait();
          } catch (InterruptedException ignored) {
          }
      }
      this.process.stdin().accept(s);
    }
  }
}
