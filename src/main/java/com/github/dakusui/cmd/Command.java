package com.github.dakusui.cmd;

import com.github.dakusui.cmd.exceptions.CommandException;
import com.github.dakusui.cmd.exceptions.CommandTimeoutException;
import com.github.dakusui.cmd.io.BasicLineReader;
import com.github.dakusui.cmd.io.LineConsumer;
import com.github.dakusui.cmd.io.LoggerLineWriter;
import com.github.dakusui.cmd.io.RingBufferedLineWriter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.concurrent.*;

public class Command {
  enum State {
    NOT_STARTED,
    STARTED,
    FINISHED
  }

  private static Logger LOGGER = LoggerFactory.getLogger(Command.class);

  private State state = State.NOT_STARTED;
  private Process proc;

  private LineConsumer stdout;

  private LineConsumer stderr;

  private int pid = -1;

  private String[] execShell;

  private String cmd;

  private RingBufferedLineWriter stdoutRingBuffer;
  private RingBufferedLineWriter stderrRingBuffer;
  private RingBufferedLineWriter stdouterrRingBuffer;

  Command(String[] execShell, String cmd) {
    this.proc = null;
    this.execShell = execShell;
    this.cmd = cmd;
  }

  public int pid() {
    if (state == State.NOT_STARTED) {
      throw new IllegalStateException(String.format("This command(%s) is not yet started.", this.cmd));
    }
    return this.pid;
  }

  private void startConsumingOutput() {
    this.stdoutRingBuffer = new RingBufferedLineWriter(100);
    this.stderrRingBuffer = new RingBufferedLineWriter(100);
    this.stdouterrRingBuffer = new RingBufferedLineWriter(100);
    this.stdout = new LineConsumer(new BasicLineReader(Charset.defaultCharset(), 0, proc.getInputStream()));
    this.stdout.addLineWriter(LoggerLineWriter.DEBUG);
    this.stdout.addLineWriter(stdoutRingBuffer);
    this.stdout.addLineWriter(stdouterrRingBuffer);
    this.stderr = new LineConsumer(new BasicLineReader(Charset.defaultCharset(), 0, proc.getErrorStream()));
    this.stderr.addLineWriter(LoggerLineWriter.DEBUG);
    this.stderr.addLineWriter(stderrRingBuffer);
    this.stderr.addLineWriter(stdouterrRingBuffer);

    this.stdout.start();
    this.stderr.start();
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
   */
  public CommandResult exec(int timeOut) throws CommandException {
    String[] execCmd = new String[this.execShell.length + 1];
    System.arraycopy(this.execShell, 0, execCmd, 0, this.execShell.length);
    execCmd[execCmd.length - 1] = this.cmd;

    try {
      this.proc = Runtime.getRuntime().exec(execCmd);
      this.startConsumingOutput();
    } catch (IOException e) {
      throw new CommandException(e);
    }
    LOGGER.debug("EXEC={}", StringUtils.join(execCmd, " "));
    this.pid = getPID(this.proc);

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
      try {
        ret = future.get(timeOut, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        throw new CommandException(e);
      } catch (ExecutionException e) {
        throw new CommandException(e);
      } catch (TimeoutException e) {
        throw new CommandTimeoutException(e);
      } finally {
        executor.shutdownNow();
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
    int exitCode;
    try {
      exitCode = Command.this.proc.waitFor();
    } catch (InterruptedException e) {
      throw new CommandException(e);
    }
    try {
      this.stderr.join();
    } catch (InterruptedException e) {
    }
    try {
      this.stdout.join();
    } catch (InterruptedException e) {
    }
    CommandResult ret = new CommandResult(
        this.cmd,
        exitCode,
        this.stdoutRingBuffer.asString(),
        this.stderrRingBuffer.asString(),
        this.stdouterrRingBuffer.asString()
    );
    return ret;
  }
}
