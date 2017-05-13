package com.github.dakusui.cmd;

import com.github.dakusui.cmd.exceptions.CommandException;
import com.github.dakusui.cmd.exceptions.CommandTimeoutException;
import com.github.dakusui.cmd.io.RingBufferedLineWriter;
import com.github.dakusui.streamablecmd.Cmd;
import com.github.dakusui.streamablecmd.exceptions.CommandExecutionException;

import java.util.concurrent.*;
import java.util.stream.Stream;

import static java.util.Arrays.asList;


public enum CommandUtils {
  ;
  private static final String[] LOCAL_SHELL = new String[] { "sh", "-c" };

  public static CommandResult runCompat(int timeout, String[] execShell, String command) throws CommandException {
    Command cmd = new Command(execShell, command);
    return cmd.exec(timeout);
  }

  public static CommandResult run(int timeOut, String[] execShell, String command) throws CommandException {
    RingBufferedLineWriter stdout = new RingBufferedLineWriter(100);
    RingBufferedLineWriter stderr = new RingBufferedLineWriter(100);
    RingBufferedLineWriter stdouterr = new RingBufferedLineWriter(100);

    Cmd cmd = new Cmd.Builder()
        .local()
        .setProgram(execShell[0])
        .addAllArguments(asList(execShell).subList(1, execShell.length))
        .addArgument(command)
        .setStdinStream(Stream.empty())
        .setStdoutConsumer(s -> {
          stdout.write(s);
          stdouterr.write(s);
        })
        .setStderrConsumer(s -> {
          stderr.write(s);
          stdouterr.write(s);
        })
        .setExitCodeChecker(value -> false)
        .build();

    final Callable<CommandResult> callable = () -> {
      try {
        cmd.run().forEach(System.out::println);
      } catch (CommandExecutionException e) {
        return new CommandResult(
            String.join(" ", e.commandLine()),
            e.exitCode(),
            stdout.asString(),
            stderr.asString(),
            stdouterr.asString()
        );
      }
      throw new RuntimeException();
    };
    if (timeOut <= 0) {
      try {
        return callable.call();
      } catch (Error | RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    } else {
      ExecutorService executor = Executors.newSingleThreadExecutor();
      Future<CommandResult> future = executor.submit(callable);
      try {
        return future.get(timeOut, TimeUnit.MILLISECONDS);
      } catch (InterruptedException | ExecutionException e) {
        throw new CommandException(e);
      } catch (TimeoutException e) {
        throw new CommandTimeoutException(e);
      } finally {
        executor.shutdownNow();
      }
    }
  }

  public static CommandResult runLocal(int timeout, String command) throws CommandException {
    return run(timeout, LOCAL_SHELL, command);
  }

  public static CommandResult runLocal(String command) throws CommandException {
    return runLocal(-1, command);
  }

  public static CommandResult runRemote(String userName, String hostName, String privKeyFile, String command) throws CommandException {
    return runRemote(-1, userName, hostName, privKeyFile, command);
  }

  public static CommandResult runRemote(int timeout, String userName, String hostName, String privKeyFile, String command) throws CommandException {
    if (privKeyFile == null) {
      return run(
          timeout,
          new String[] {
              "ssh", "-o", "StrictHostKeyChecking=no",
              String.format("%s@%s", userName, hostName)
          },
          command
      );
    }

    return run(
        timeout,
        new String[] {
            "ssh", "-i", privKeyFile,
            "-o", "StrictHostKeyChecking=no",
            String.format("%s@%s", userName, hostName)
        },
        command
    );
  }
}
