package com.github.dakusui.cmd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Command {
	static enum State {
		NOT_STARTED,
		STARTED,
		FINISHED;
	}
	
	public static enum SourceType {
		STDOUT,
		STDERR
	}
	
	public static class Line {
		SourceType sourceType;
		String body;
		public Line(SourceType sourceType, String body) {
			this.sourceType = sourceType;
			this.body = body;
		}
		
		public String toString() {
			return String.format("%s %s", this.sourceType, body);
		}
	}
	
	public static class Buffer {
		private static Logger LOGGER = LoggerFactory.getLogger(Buffer.class);
		private int numLines = 0;
		Line[] ring;
		int    cur;

		Buffer(int i) {
			ring = new Line[i];
			cur = 0;
		}
		
		public void write(Line l) {
			LOGGER.trace("{} {}", l.sourceType, l.body);
			writeEngine(l);
			numLines++;
		}
		
		public void writeEngine(Line l) {
			ring[this.cur++] = l;
			this.cur = this.cur % ring.length;
		}
		
		
		public String asString(SourceType sourceType) {
			StringBuffer b = new StringBuffer();
			for (int i = 0; i < this.ring.length; i++) {
				Line l = this.ring[(this.cur + i) % this.ring.length];
				if (l == null) continue;
				if (sourceType == null || l.sourceType == sourceType) {
					b.append(l.body);
					if (i != this.ring.length - 1) {
						b.append(EOL);
					}
				}
			}
			return b.toString();
		}
		
		public int numLines() {
			return this.numLines;
		}
		
		public String toString() {
			return asString(null);
		}
	}
	
	private static Logger LOGGER = LoggerFactory.getLogger(Command.class);
	
	private static String EOL = System.getProperty("line.separator");

	private State state = State.NOT_STARTED;
	private Process proc;

	private BufferedReader stdout;

	private BufferedReader stderr;
	
	private List<BufferedReader> readers = new LinkedList<BufferedReader>();
	
	private List<BufferedReader> finishedReaders = new LinkedList<BufferedReader>();

	private Buffer buffer;

	private int pid = -1;

	private String[] execShell;

	private String cmd;

	private String[] stopShell;

	private List<CommandListener> listeners;
	
	public Command(String[] execShell, String cmd, String[] stopShell, List<CommandListener> listeners) {
		this.proc = null;
		this.execShell = execShell;
		this.stopShell = stopShell;
		this.cmd = cmd;
		this.listeners = listeners;
		this.buffer = new Buffer(100);
	}
	
	public int pid() {
		if (state == State.NOT_STARTED) {
			throw new IllegalStateException(String.format("This command(%s) is not yet started.", this.cmd));
		}
		return this.pid;
	}

	public CommandResult exec(int timeOut, String[]... stdins) throws InterruptedException, ExecutionException, TimeoutException, IOException { // throws Exception {
		String[] execCmd = new String[this.execShell.length + 1];
		System.arraycopy(this.execShell, 0, execCmd, 0, this.execShell.length);
		execCmd[execCmd.length - 1] = this.cmd;

		try {
			LOGGER.debug("EXEC={}", join(" ", execCmd));
			this.proc = Runtime.getRuntime().exec(execCmd);
			Field f = this.proc.getClass().getDeclaredField("pid");
			f.setAccessible(true);
			this.pid = Integer.parseInt(f.get( this.proc ).toString());
			this.stdout = new BufferedReader(new InputStreamReader(this.proc.getInputStream()));
			this.stderr = new BufferedReader(new InputStreamReader(this.proc.getErrorStream()));
			readers.add(stdout);
			readers.add(stderr);
			this.state = State.STARTED;
			for (CommandListener l : this.listeners) {
				l.exec(this);
			}
		} catch (NumberFormatException e) {
			String msg = "This program cannot be run on this platform.";
			LOGGER.error(msg, e);
			throw new Error(e);
		} catch (IllegalAccessException e) {
			String msg = "This program cannot be run on this platform.";
			LOGGER.error(msg, e);
			throw new Error(e);
		} catch (NoSuchFieldException e) {
			String msg = "This program cannot be run on this platform.";
			LOGGER.error(msg, e);
			throw new Error(e);
		} catch (SecurityException e) {
			String msg = "This program cannot be run on this platform.";
			LOGGER.error(msg, e);
			throw new Error(e);
		}
		LOGGER.debug("pid={}", this.pid);

		final Callable<CommandResult> callable = new Callable<CommandResult>() {
			public CommandResult call() throws InterruptedException, IOException {
				return Command.this.waitFor();
			}
		};
		CommandResult ret;
		if (timeOut <= 0) {
			ret = this.waitFor(stdins);
		} else {
			ExecutorService executor = Executors.newSingleThreadExecutor();
			Future<CommandResult> future = executor.submit(callable);
			boolean finished = false;
			try {
				ret = future.get(timeOut, TimeUnit.MILLISECONDS);
				finished = true;
			} finally {
				if (!finished) {
					String[] killCmd = new String[this.stopShell.length + 1];
					System.arraycopy(this.stopShell, 0, killCmd, 0, this.stopShell.length);
					killCmd[killCmd.length - 1] = String.format("kill -9 %s", this.pid);
					LOGGER.debug("KILL={}", join(" ", killCmd));
					this.proc = Runtime.getRuntime().exec(killCmd);
				}
			}
		}
		this.state = State.FINISHED;
		return ret;
	}
	
	private CommandResult waitFor(String[]... stdins) throws IOException, InterruptedException {
		Line line;
		PrintStream ps = new PrintStream(Command.this.proc.getOutputStream());
		while ((line = read()) != null) {
			Command.this.buffer.write(line);
		}
		for (String[] stdin : stdins) {
			for (String l : stdin) {
				ps.println(l);
			}
			ps.flush();
			while ((line = read()) != null) {
				Command.this.buffer.write(line);
			}
		}
		int exitCode = Command.this.proc.waitFor();
		while ((line = read()) != null) {
			Command.this.buffer.write(line);
		}
		CommandResult ret = new CommandResult(exitCode, Command.this.buffer);
		return ret;
	}

	private Line read() throws IOException {
		Line ret = null;
		int wait = 0;
		// On Mac, without this retry logic, this method blocks when we read a big data (> 10kb).
		while (wait <= 12 && (ret = read(wait)) == null) {
			wait += 4;
		}
		return ret;
	}
	
	private Line read(int wait) throws IOException {
		Line ret = null;
		for (BufferedReader r : readers) {
			try {
				Thread.sleep(wait);
			} catch (Exception e) {
			}	
			SourceType sourceType = sourceTypeOf(r);
			LOGGER.trace("Trying {}", sourceType);
			if (r.ready()) {
				LOGGER.trace("   {} is ready.", sourceType);
				String l = r.readLine();
				if (l == null) {
					finishedReaders.add(r);
				}
				
				LOGGER.debug("{}, {}", sourceType, l);
				ret = new Line(sourceType, l);
			}
		}
		LOGGER.trace("Line read:{}", ret);
		return ret;
	}
	
	private SourceType sourceTypeOf(BufferedReader r) {
		SourceType type;
		if (r == this.stdout) {
			type = SourceType.STDOUT;
		} else if (r == this.stderr) {
			type = SourceType.STDERR;
		} else {
			throw new RuntimeException("Unknown input stream is found.");
		}		
		return type;
	}
	
	private static String join(String sep, String[] strings) {
		StringBuffer b = new StringBuffer();
		boolean firstTime = true;
		for (String s : strings) {
			if (firstTime) {
				firstTime = false;;
			} else {
				b.append(" ");
			}
			b.append(s);
		}
		return b.toString();
	}
}
