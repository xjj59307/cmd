package com.github.dakusui.cmd;


@Deprecated
public class CommandResult {

  private int    exitCode;
  private String commandLine;
  private String stdout;
  private String stderr;
  private String stdouterr;

  public CommandResult(String commandLine, int exitCode, String stdout, String stderr, String stdouterr) {
    this.commandLine = commandLine;
    this.exitCode = exitCode;
    this.stdout = stdout;
    this.stderr = stderr;
    this.stdouterr = stdouterr;
  }

  public String stdout() {
    return this.stdout;
  }

  public String stderr() {
    return this.stderr;
  }

  public String stdouterr() {
    return this.stdouterr;
  }

  public int exitCode() {
    return this.exitCode;
  }

  public String commandLine() {
    return this.commandLine;
  }

}
