package com.github.dakusui.cmd;

import com.github.dakusui.cmd.exceptions.CommandException;
import com.github.dakusui.cmd.exceptions.CommandTimeoutException;
import com.github.dakusui.cmd.io.BasicLineReader;
import com.github.dakusui.cmd.io.LineConsumer;
import com.github.dakusui.cmd.io.LineWriter;
import com.github.dakusui.cmd.io.LoggerLineWriter;
import com.github.dakusui.cmd.io.RingBufferedLineWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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
			throw new IllegalStateException(String.format("This command(%s) is not yet started.", cmd));
		}
		return pid;
	}

	private void consumeStreamInBackground(Iterable<LineWriter> stdoutWriters, Iterable<LineWriter> stderrWriters) {
		stdout = new LineConsumer(new BasicLineReader(Charset.defaultCharset(), proc.getInputStream()));
		stdoutWriters.forEach(stdout::addLineWriter);
		stdout.addLineWriter(LoggerLineWriter.DEBUG);

		stderr = new LineConsumer(new BasicLineReader(Charset.defaultCharset(), proc.getErrorStream()));
		stderrWriters.forEach(stderr::addLineWriter);
		stderr.addLineWriter(LoggerLineWriter.DEBUG);

		stdout.start();
		stderr.start();
	}

	public Stream<String> execAsync() {
		execCmdInBackground();

		// Optional.empty is a poison object.
		BlockingQueue<Optional<String>> queue = new ArrayBlockingQueue<>(100);
		AtomicBoolean finishOnce = new AtomicBoolean(false);

		LineWriter writer = new LineWriter() {
			@Override
			public void write(String line) {
				try {
					queue.put(Optional.of(line));
				} catch (InterruptedException exception) {
					Thread.currentThread().interrupt();
					throw new CommandException(exception);
				}
			}

			@Override
			public void finish() {
				try {
					synchronized (finishOnce) {
						if (finishOnce.get()) {
							queue.put(Optional.empty());
						} else {
							finishOnce.set(true);
						}
					}
				} catch (InterruptedException exception) {
					Thread.currentThread().interrupt();
					throw new CommandException(exception);
				}
			}
		};

		consumeStreamInBackground(
				Collections.singletonList(writer),
				Collections.singletonList(writer)
		);

		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new Iterator<String>() {
			private String line;

			@Override
			public boolean hasNext() {
				try {
					Optional<String> optional = queue.take();
					optional.ifPresent((value) -> line = value);
					return optional.isPresent();
				} catch (InterruptedException exception) {
					Thread.currentThread().interrupt();
					throw new CommandException(exception);
				}
			}

			@Override
			public String next() {
				return line;
			}
		}, Spliterator.IMMUTABLE), false);
	}

	private void execCmdInBackground() {
		String[] execCmd = new String[execShell.length + 1];
		System.arraycopy(execShell, 0, execCmd, 0, execShell.length);
		execCmd[execCmd.length - 1] = cmd;

		try {
			proc = Runtime.getRuntime().exec(execCmd);
			pid = getPID();
		} catch (IOException e) {
			throw new CommandException(e);
		}
	}

	public CommandResult exec(int timeOut) throws CommandException {
		execCmdInBackground();

		stdoutRingBuffer = new RingBufferedLineWriter(100);
		stderrRingBuffer = new RingBufferedLineWriter(100);
		stdouterrRingBuffer = new RingBufferedLineWriter(100);
		consumeStreamInBackground(
				Arrays.asList(stdoutRingBuffer, stdouterrRingBuffer),
				Arrays.asList(stderrRingBuffer, stdouterrRingBuffer)
		);

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
				ret = Integer.parseInt(f.get(proc).toString());
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
					cmd,
					exitCode,
					stdoutRingBuffer.asString(),
					stderrRingBuffer.asString(),
					stdouterrRingBuffer.asString()
			);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new CommandException(e);
		}
	}
}
