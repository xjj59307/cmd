package com.github.dakusui.cmd.io;

import com.github.dakusui.cmd.exceptions.CommandException;

public interface LineReader {

	String read() throws CommandException;

	void close() throws CommandException;
}
