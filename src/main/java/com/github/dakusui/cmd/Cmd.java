package com.github.dakusui.cmd;

import com.github.dakusui.cmd.core.StreamableProcess;
import com.github.dakusui.cmd.exceptions.Exceptions;
import com.github.dakusui.cmd.exceptions.UnexpectedExitValueException;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    return run(shell, processConfigBuilder(Stream.empty()).build(), commandLine);
  }

  static Stream<String> run(Shell shell, StreamableProcess.Config config, String... commandLine) {
    return new Cmd.Builder()
        .addAll(asList(commandLine))
        .withShell(shell)
        .configure(config)
        .build()
        .run();
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
    static final Object SENTINEL = new Object();

    private final String[]                 shell;
    private final String                   command;
    private       StreamableProcess        process;
    private final StreamableProcess.Config processConfig;

    Impl(String[] shell, String command, StreamableProcess.Config config) {
      this.shell = shell;
      this.command = command;
      this.processConfig = config;
    }

    @Override
    public synchronized Stream<String> run() {
      ExecutorService excutorService = Executors.newFixedThreadPool(3);
      process = startProcess(this.shell, this.command, this.processConfig, excutorService);
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

    private static StreamableProcess startProcess(String[] shell, String command, StreamableProcess.Config processConfig, ExecutorService executorService) {
      return new StreamableProcess(
          createProcess(shell, command),
          executorService,
          processConfig
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

}
