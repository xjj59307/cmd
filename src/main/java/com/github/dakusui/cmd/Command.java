package com.github.dakusui.cmd;

import com.github.dakusui.cmd.exceptions.CommandException;
import com.github.dakusui.cmd.exceptions.CommandTimeoutException;
import com.github.dakusui.cmd.io.BasicLineReader;
import com.github.dakusui.cmd.io.LineConsumer;
import com.github.dakusui.cmd.io.LoggerLineWriter;
import com.github.dakusui.cmd.io.RingBufferedLineWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

public class Command {

	private static Logger LOGGER = LoggerFactory.getLogger(Command.class);

	private int pid = -1;

	private Process proc;

	private LineConsumer stdout;

	private LineConsumer stderr;

	private String[] execShell;

	private String cmd;

	private RingBufferedLineWriter stdoutRingBuffer;

	private RingBufferedLineWriter stderrRingBuffer;

	private RingBufferedLineWriter stdouterrRingBuffer;

	public Command(String[] execShell, String cmd) {
		this.execShell = execShell;
		this.cmd = cmd;
	}

	public int getPid() {
		if (pid == -1) {
			throw new IllegalStateException(String.format("This command(%s) is not yet started.", this.cmd));
		}
		return this.pid;
	}

	private void startConsumingOutput() {
		this.stdoutRingBuffer = new RingBufferedLineWriter(100);
		this.stderrRingBuffer = new RingBufferedLineWriter(100);
		this.stdouterrRingBuffer = new RingBufferedLineWriter(100);

		this.stdout = new LineConsumer(new BasicLineReader(Charset.defaultCharset(), proc.getInputStream()));
		Stream.of(
				LoggerLineWriter.DEBUG,
				stdoutRingBuffer,
				stdouterrRingBuffer
		).forEach(this.stdout::addLineWriter);

		this.stderr = new LineConsumer(new BasicLineReader(Charset.defaultCharset(), proc.getErrorStream()));
		Stream.of(
				LoggerLineWriter.DEBUG,
				stderrRingBuffer,
				stdouterrRingBuffer
		).forEach(this.stderr::addLineWriter);

		this.stdout.start();
		this.stderr.start();
	}

	public CommandResult exec(int timeOut) throws CommandException {
		String[] execCmd = new String[execShell.length + 1];
		System.arraycopy(this.execShell, 0, execCmd, 0, this.execShell.length);
		execCmd[execCmd.length - 1] = cmd;

		try {
			proc = Runtime.getRuntime().exec(execCmd);
			pid = getPID();
			startConsumingOutput();
		} catch (IOException e) {
			throw new CommandException(e);
		}

		if (timeOut <= 0) {
			return waitFor();
		} else {
			ExecutorService executor = Executors.newSingleThreadExecutor();
			Future<CommandResult> future = executor.submit(this::waitFor);

			try {
				return future.get(timeOut, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new CommandException(e);
			} catch (ExecutionException e) {
				throw new CommandException(e);
			} catch (TimeoutException e) {
				throw new CommandTimeoutException(e);
			} finally {
				executor.shutdownNow();
			}
		}
	}

	private int getPID() {
		int ret = -1;
		try {
			Field f = proc.getClass().getDeclaredField("pid");
			f.setAccessible(true);
			try {
				ret = Integer.parseInt(f.get(this.proc).toString());
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
		try {
			int exitCode = proc.waitFor();
			stderr.join();
			stdout.join();

			return new CommandResult(
					this.cmd,
					exitCode,
					this.stdoutRingBuffer.asString(),
					this.stderrRingBuffer.asString(),
					this.stdouterrRingBuffer.asString()
			);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new CommandException(e);
		}
	}
}
