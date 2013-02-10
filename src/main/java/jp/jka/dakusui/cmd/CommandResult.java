package jp.jka.dakusui.cmd;

import jp.jka.dakusui.cmd.Command.Buffer;
import jp.jka.dakusui.cmd.Command.SourceType;

public class CommandResult {

	private Buffer buffer;
	private int    exitCode;

	public CommandResult(int exitCode, Buffer buffer) {
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
	
}
