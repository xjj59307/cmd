package com.github.dakusui.cmd.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import com.github.dakusui.cmd.exceptions.CommandException;

public class BasicLineReader implements LineReader {

	private BufferedReader reader;

	public BasicLineReader(Charset cs, InputStream is) {
		this.reader = new BufferedReader(new InputStreamReader(is, cs));
	}

	@Override
	public String read() throws CommandException {
		try {
			return reader.readLine();
		} catch (IOException e) {
			throw new CommandException(e);
		}
	}

	@Override
	public void close() throws CommandException {
		try {
			reader.close();
		} catch (IOException e) {
			throw new CommandException(e);
		}
	}
}
