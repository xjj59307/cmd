package com.github.dakusui.cmd.io;

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dakusui.cmd.exceptions.CommandException;

public class LineConsumer extends Thread {
	private static final Logger LOGGER = LoggerFactory.getLogger(LineConsumer.class);
	private LineReader reader;
	private List<LineWriter> writers = new LinkedList<LineWriter>();
	/*
	 * This library doesn't respect the second byte of EOL string of the platform.
	 */
	static final char EOL = System.getProperty("line.separator").toCharArray()[0];

	public LineConsumer(LineReader reader) {
		this.reader = reader;
	}
	
	public void run() {
		try {
			while (consumeLine()) {}
		} catch (CommandException e) {
			LOGGER.error("Error detected while consuming command output:'{}'", e.getMessage());
			LOGGER.trace(e.getMessage(), e);
		} finally {
			try {
				this.reader.close();
			} catch (CommandException e) {
				LOGGER.error("Error detected while closing command output stream:'{}'", e.getMessage());
				LOGGER.trace(e.getMessage(), e);
			}
		}
	}
	
	public void addLineWriter(LineWriter writer) {
		this.writers.add(writer);
	}
	
	boolean consumeLine() throws CommandException {
		String line = reader.read();
		if (line == null) return false;
		for (LineWriter w : this.writers) {
			w.write(line);
		}
		return true;
	}
}
