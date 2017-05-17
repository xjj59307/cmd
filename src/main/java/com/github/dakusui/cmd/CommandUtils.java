package com.github.dakusui.cmd;

import com.github.dakusui.cmd.exceptions.CommandException;
import com.github.dakusui.cmd.exceptions.CommandTimeoutException;
import com.github.dakusui.cmd.io.RingBufferedLineWriter;
import com.github.dakusui.streamablecmd.Cmd;
import com.github.dakusui.streamablecmd.exceptions.CommandExecutionException;
import com.github.dakusui.streamablecmd.exceptions.Exceptions;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static java.util.Arrays.asList;


public enum CommandUtils {
  ;
  private static final String[] LOCAL_SHELL = new String[] { "sh", "-c" };

  public static CommandResult run(int timeOut, String[] execShell, String command) throws CommandException {
    return run(
        timeOut,
        new Cmd.Shell.Builder.ForLocal()
            .withProgram(execShell[0])
            .clearOptions()
            .addAllOptions(asList(execShell).subList(1, execShell.length))
            .build(),
        command
    );
  }

  public static CommandResult run(int timeOut, Cmd.Shell shell, String command) throws CommandException {
    RingBufferedLineWriter stdout = new RingBufferedLineWriter(100);
    RingBufferedLineWriter stderr = new RingBufferedLineWriter(100);
    RingBufferedLineWriter stdouterr = new RingBufferedLineWriter(100);
    AtomicReference<Integer> exitValueHolder = new AtomicReference<>(null);
    Cmd cmd = new Cmd.Builder()
        .withShell(shell)
        .add(command)
        .configure(new Cmd.Io.Base(Stream.empty()) {
          @Override
          public boolean redirectsStdout() {
            return true;
          }

          @Override
          public boolean redirectsStderr() {
            return false;
          }

          @Override
          protected void consumeStdout(String s) {
            stdout.write(s);
            stdouterr.write(s);
          }

          @Override
          protected void consumeStderr(String s) {
            stderr.write(s);
            stdouterr.write(s);
          }

          @Override
          protected boolean exitValue(int exitValue) {
            synchronized (exitValueHolder) {
              exitValueHolder.set(exitValue);
              exitValueHolder.notifyAll();
              return exitValue == 0;
            }
          }
        })
        .build();

    final Callable<CommandResult> callable = () -> {
      String commandLine = String.join(" ", cmd.getCommandLine());
      try {
        cmd.run().forEach(s -> {
        });
        synchronized (exitValueHolder) {
          Integer exitValue;
          while ((exitValue = exitValueHolder.get()) == null) {
            try {
              exitValueHolder.wait();
            } catch (InterruptedException ignored) {
            }
          }
          return new CommandResult(
              String.join(" "),
              exitValue,
              stdout.asString(),
              stderr.asString(),
              stdouterr.asString()
          );
        }
      } catch (CommandExecutionException e) {
        return new CommandResult(
            commandLine,
            e.exitCode(),
            stdout.asString(),
            stderr.asString(),
            stdouterr.asString()
        );
      }
    };
    if (timeOut <= 0) {
      try {
        return callable.call();
      } catch (Error | Exception e) {
        throw Exceptions.wrap(e);
      }
    } else {
      ExecutorService executor = Executors.newSingleThreadExecutor();
      Future<CommandResult> future = executor.submit(callable);
      try {
        return future.get(timeOut, TimeUnit.MILLISECONDS);
      } catch (InterruptedException | ExecutionException e) {
        throw Exceptions.wrap(e);
      } catch (TimeoutException e) {
        throw new CommandTimeoutException(e.getMessage(), e);
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
