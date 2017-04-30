package com.github.dakusui.cmd.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum LoggerLineWriter implements LineWriter {
	ERROR {
		@Override
		public void write(String line) {
			LOGGER.error(line);
		}
	},		
	WARN {
		@Override
		public void write(String line) {
			LOGGER.warn(line);
		}
	},
	INFO {
		@Override
		public void write(String line) {
			LOGGER.info(line);
		}
	},
	DEBUG {
		@Override
		public void write(String line) {
			LOGGER.debug(line);
		}
	},
	TRACE {
		@Override
		public void write(String line) {
			LOGGER.trace(line);
		}
	};

	private static final Logger LOGGER = LoggerFactory.getLogger(LoggerLineWriter.class);
}