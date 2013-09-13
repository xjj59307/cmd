package com.github.dakusui.cmd.exceptions;

/**
 * An exception class to indicate a misuse of this library is detected.
 * 
 * @author ukaihiroshi01
 */
public class CommandRuntimeException extends RuntimeException {
	/**
	 * A serial version UID
	 */
	private static final long serialVersionUID = -2128273730680871415L;

	/**
	 * Creates an object of this class.
	 */
	public CommandRuntimeException() {
		this((String)null);
	}

	/**
	 * Creates an object of this class.
	 * @param msg A message string to be set.
	 */
	public CommandRuntimeException(String msg) {
		super(msg);
	}

	/**
	 * Creates an object of this class.
	 * @param t
	 */
	public CommandRuntimeException(Throwable t) {
		this(null, t);
	}
	
	/**
	 * Creates an object of this class.
	 * 
	 * @param msg A message string to be set.
	 * @param t
	 */
	public CommandRuntimeException(String msg, Throwable t) {
		super(msg != null ? msg + "(" + t.getMessage() + ")" : t.getMessage());
		this.setStackTrace(t.getStackTrace());
	}
}
