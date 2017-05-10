package com.github.dakusui.cmd.streamable;

import java.util.stream.Stream;

public class SshException extends RuntimeException {
  final private int            exitCode;
  final private Stream<String> stderr;
  private final String commandLine;

  public SshException(int exitCode, String commandLine, Stream<String> stderr) {
    this.exitCode = exitCode;
    this.stderr = stderr;
    this.commandLine = commandLine;
  }

  public Stream<String> stderr() {
    return this.stderr;
  }

  public int exitCode() {
    return this.exitCode;
  }
}
