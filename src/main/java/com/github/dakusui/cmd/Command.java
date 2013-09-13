package com.github.dakusui.cmd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

import com.github.dakusui.cmd.exceptions.CommandException;
import com.github.dakusui.cmd.exceptions.CommandRuntimeException;
import com.github.dakusui.cmd.exceptions.CommandTimeoutException;
import com.github.dakusui.cmd.io.CommandSink;
import com.github.dakusui.cmd.io.Line;

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
	
	private static Logger LOGGER = LoggerFactory.getLogger(Command.class);
	
	private State state = State.NOT_STARTED;
	private Process proc;

	private BufferedReader stdout;

	private BufferedReader stderr;
	
	private List<BufferedReader> readers = new LinkedList<BufferedReader>();
	
	private List<BufferedReader> finishedReaders = new LinkedList<BufferedReader>();

	private CommandSink buffer;

	private int pid = -1;

	private String[] execShell;

	private String cmd;

	private String[] stopShell;

	Command(String[] execShell, String cmd, String[] stopShell) {
		this.proc = null;
		this.execShell = execShell;
		this.stopShell = stopShell;
		this.cmd = cmd;
		this.buffer = new CommandSink(100);
	}
	
	public int pid() {
		if (state == State.NOT_STARTED) {
			throw new IllegalStateException(String.format("This command(%s) is not yet started.", this.cmd));
		}
		return this.pid;
	}

	/**
	 * Executes this command in a synchronous manner.
	 * if <code>timeOut</code> is set to a value less than or equal to zero, this method
	 * does never time out.
	 * Otherwise times out in <code>timeOut</code> milliseconds.
	 * 
	 * @param timeOut duration 
	 * @return
	 * @throws CommandException The command failed during execution. 
	 * @throws CommandRuntimeException The command couldn't be executed or this platform isn't suitable for this library.
	 */
	public CommandResult exec(int timeOut) throws CommandException {
		String[] execCmd = new String[this.execShell.length + 1];
		System.arraycopy(this.execShell, 0, execCmd, 0, this.execShell.length);
		execCmd[execCmd.length - 1] = this.cmd;

		try {
			this.proc = Runtime.getRuntime().exec(execCmd);
		} catch (IOException e) {
			throw new CommandException(e);
		}
		LOGGER.debug("EXEC={}", join(" ", execCmd));
		this.pid = getPID(this.proc);
		
		this.stdout = new BufferedReader(new InputStreamReader(this.proc.getInputStream()));
		this.stderr = new BufferedReader(new InputStreamReader(this.proc.getErrorStream()));
		readers.add(stdout);
		readers.add(stderr);
		this.state = State.STARTED;
		LOGGER.debug("pid={}", this.pid);

		final Callable<CommandResult> callable = new Callable<CommandResult>() {
			public CommandResult call() throws CommandException {
				return Command.this.waitFor();
			}
		};
		CommandResult ret;
		if (timeOut <= 0) {
			ret = this.waitFor();
		} else {
			ExecutorService executor = Executors.newSingleThreadExecutor();
			Future<CommandResult> future = executor.submit(callable);
			boolean finished = false;
			try {
				ret = future.get(timeOut, TimeUnit.MILLISECONDS);
				finished = true;
			} catch (InterruptedException e) {
				throw new CommandException(e);
			} catch (ExecutionException e) {
				throw new CommandException(e);
			} catch (TimeoutException e) {
				throw new CommandTimeoutException(e);
			} finally {
				if (!finished) {
					String[] killCmd = new String[this.stopShell.length + 1];
					System.arraycopy(this.stopShell, 0, killCmd, 0, this.stopShell.length);
					killCmd[killCmd.length - 1] = String.format("kill -9 %s", this.pid);
					LOGGER.debug("KILL={}", join(" ", killCmd));
					try {
						this.proc = Runtime.getRuntime().exec(killCmd);
					} catch (Throwable t) {
						LOGGER.error("Since the command was started but not finished, commandrunner tried to kill it. Unfortunatelly it couldn't even kill the process.");
						LOGGER.trace("This exception is ignored. {}", t);
					}
				}
			}
		}
		this.state = State.FINISHED;
		return ret;
	}
	
	private int getPID(Process proc) {
		int ret = -1;
		try {
			Field f = proc.getClass().getDeclaredField("pid");
			f.setAccessible(true);
			try {
				ret = Integer.parseInt(f.get( this.proc ).toString());
			} finally {
				f.setAccessible(false);
			}
		} catch (IllegalAccessException e) {
			assert false;
			String msg = "PID isn't available on this platform. (IllegalAccess) '-1' is used insted.";
			LOGGER.debug(msg);
		} catch (NoSuchFieldException e) {
			String msg = "PID isn't available on this platform. (NoSuchField) '-1' is used insted.";
			LOGGER.debug(msg);
		} catch (SecurityException e) {
			String msg = "PID isn't available on this platform. (Security) '-1' is used insted.";
			LOGGER.debug(msg);
		} catch (NumberFormatException e) {
			String msg = "PID isn't available on this platform. (NumberFormat) '-1' is used insted.";
			LOGGER.debug(msg);
		}
		return ret;
	}
	
	private CommandResult waitFor() throws CommandException {
		Line line;
		while ((line = read()) != null) {
			Command.this.buffer.write(line);
		}
		int exitCode;
		try {
			exitCode = Command.this.proc.waitFor();
		} catch (InterruptedException e) {
			throw new CommandException(e);
		}
		while ((line = read()) != null) {
			Command.this.buffer.write(line);
		}
		CommandResult ret = new CommandResult(this.cmd, exitCode, Command.this.buffer);
		return ret;
	}

	private Line read() throws CommandException {
		Line ret = null;
		int wait = 0;
		// On Mac, without this retry logic, this method blocks when we read a big data (> 10kb).
		while (wait <= 12 && (ret = read(wait)) == null) {
			wait += 4;
		}
		return ret;
	}
	
	private Line read(int wait) throws CommandException {
		Line ret = null;
		for (BufferedReader r : readers) {
			try {
				Thread.sleep(wait);
			} catch (InterruptedException e) {
				throw new CommandException(e);
			}	
			SourceType sourceType = sourceTypeOf(r);
			LOGGER.trace("Trying {}", sourceType);
			try {
				if (r.ready()) {
					LOGGER.trace("   {} is ready.", sourceType);
					String l = r.readLine();
					if (l == null) {
						finishedReaders.add(r);
					}
					
					LOGGER.debug("{}, {}", sourceType, l);
					ret = new Line(sourceType, l);
				}
			} catch (IOException e) {
				throw new CommandException(e);
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
