package com.github.dakusui.cmd;

import com.github.dakusui.cmd.Command.SourceType;
import com.github.dakusui.cmd.io.CommandSink;

public class CommandResult {

	private CommandSink buffer;
	private int    exitCode;
	private String commandLine;

	public CommandResult(String commandLine, int exitCode, CommandSink buffer) {
		this.commandLine = commandLine;
		this.buffer = buffer;
		this.exitCode = exitCode;
	}
	
	public String stdout() {
		return this.buffer.asString(SourceType.STDOUT);
	}

	public String stderr() {
		return this.buffer.asString(SourceType.STDERR);
	}

	public String asString() {
		return this.buffer.asString(null);
	}
	
	public int exitCode() {
		return this.exitCode;
	}
	
	public String commandLine() {
		return this.commandLine;
	}
	
}
