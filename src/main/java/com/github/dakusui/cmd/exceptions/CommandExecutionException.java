package com.github.dakusui.cmd.exceptions;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Thrown when a command executed by 'streamable-cmd' exits with an error.
 */
public class CommandExecutionException extends BaseException {
  final private int    exitCode;
  private final String commandLine;
  private final int    pid;

  public CommandExecutionException(int exitCode, String commandLine, int pid) {
    super(composeErrorMessage(exitCode, commandLine, pid));
    this.exitCode = exitCode;
    this.pid = pid;
    this.commandLine = commandLine;
  }

  public int exitCode() {
    return this.exitCode;
  }

  public String commandLine() {
    return this.commandLine;
  }

  public int pid() {
    return this.pid;
  }

  private static String composeErrorMessage(int exitCode, String commandLine, int pid) {
    return String.format(
        "Command line:[%s](pid=%s) exit with '%s'",
        Stream.of(commandLine).collect(Collectors.joining(" ")),
        pid,
        exitCode
    );
  }
}
