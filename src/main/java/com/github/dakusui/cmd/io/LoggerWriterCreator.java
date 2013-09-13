package com.github.dakusui.cmd.io;

import org.slf4j.Logger;

public enum LoggerWriterCreator {
	ERROR {
		@Override
		public LineWriter lineWriter(final Logger logger) {
			return new LineWriter() {
				@Override
				public void write(String line) {
					logger.error(line);
				}
			};
		}
	},		
	WARN {
		@Override
		public LineWriter lineWriter(final Logger logger) {
			return new LineWriter() {
				@Override
				public void write(String line) {
					logger.warn(line);
				}
			};
		}
	},
	INFO {
		@Override
		public LineWriter lineWriter(final Logger logger) {
			return new LineWriter() {
				@Override
				public void write(String line) {
					logger.info(line);
				}
			};
		}
	},
	DEBUG {
		@Override
		public LineWriter lineWriter(final Logger logger) {
			return new LineWriter() {
				@Override
				public void write(String line) {
					logger.debug(line);
				}
			};
		}
	},
	TRACE {
		@Override
		public LineWriter lineWriter(final Logger logger) {
			return new LineWriter() {
				@Override
				public void write(String line) {
					logger.trace(line);
				}
			};
		}
	};
	public abstract LineWriter lineWriter(Logger logger);
}