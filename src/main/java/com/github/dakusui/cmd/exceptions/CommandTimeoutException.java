package com.github.dakusui.cmd.exceptions;

public class CommandTimeoutException extends CommandException {
	/**
	 * Serial version UIT.x
	 */
	private static final long serialVersionUID = 6202136371314715671L;

	public CommandTimeoutException() {
		super();
	}

	public CommandTimeoutException(String msg) {
		super(msg);
	}

	public CommandTimeoutException(Throwable t) {
		super(t);
	}

	public CommandTimeoutException(String msg, Throwable t) {
		super(msg, t);
	}
}
