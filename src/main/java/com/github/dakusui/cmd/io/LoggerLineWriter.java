package com.github.dakusui.cmd.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum LoggerLineWriter implements LineWriter {
	DEBUG {
		@Override
		public void write(String line) {
			LOGGER.debug(line);
		}
	};

	private static final Logger LOGGER = LoggerFactory.getLogger(LoggerLineWriter.class);

	@Override
	public void finish() {}
}