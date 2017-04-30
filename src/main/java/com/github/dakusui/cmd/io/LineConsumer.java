package com.github.dakusui.cmd.io;

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dakusui.cmd.exceptions.CommandException;

public class LineConsumer extends Thread {

	private static final Logger LOGGER = LoggerFactory.getLogger(LineConsumer.class);

	private LineReader reader;

	private List<LineWriter> writers = new LinkedList<>();

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

	private boolean consumeLine() throws CommandException {
		String line = reader.read();

		if (line == null) {
			writers.forEach(LineWriter::finish);
			return false;
		}

		writers.forEach((writer) -> writer.write(line));

		return true;
	}
}
