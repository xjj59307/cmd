package com.github.dakusui.cmd.core;

import com.github.dakusui.cmd.Cmd;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.util.Arrays.asList;

public class StreamableProcess extends Process {
  private final Process          process;
  private final Stream<String>   stdout;
  private final Stream<String>   stderr;
  private final Consumer<String> stdin;
  private final Selector<String> selector;


  public StreamableProcess(Process process, ExecutorService executorService, Cmd.Io io) {
    this.process = process;
    this.stdout = IoUtils.toStream(getInputStream(), io.charset());
    this.stderr = IoUtils.toStream(getErrorStream(), io.charset());
    this.stdin = IoUtils.toConsumer(this.getOutputStream(), io.charset());
    this.selector = createSelector(io, executorService);
  }

  @Override
  public OutputStream getOutputStream() {
    return process.getOutputStream();
  }

  @Override
  public InputStream getInputStream() {
    return process.getInputStream();
  }

  @Override
  public InputStream getErrorStream() {
    return process.getErrorStream();
  }

  @Override
  public int waitFor() throws InterruptedException {
    return process.waitFor();
  }

  @Override
  public int exitValue() {
    return process.exitValue();
  }

  @Override
  public void destroy() {
    process.destroy();
    for (Stream<String> eachStream : asList(this.stdout, this.stderr)) {
      eachStream.close();
    }
    this.selector.close();
  }

  /**
   * Returns a {@code Stream<String>} object that represents standard output
   * of the underlying process.
   */
  public Stream<String> stdout() {
    return this.stdout;
  }

  /**
   * Returns a {@code Stream<String>} object that represents standard error
   * of the underlying process.
   */
  public Stream<String> stderr() {
    return this.stderr;
  }

  /**
   * Returns a {@code Consumer<String>} object that represents standard input
   * of the underlying process.
   */
  public Consumer<String> stdin() {
    return this.stdin;
  }

  public int getPid() {
    return getPid(this.process);
  }

  private Selector<String> createSelector(Cmd.Io io, ExecutorService excutorService) {
    return new Selector.Builder<String>()
        .add(io.stdin(), this.stdin())
        .add(
            this.stdout()
                .map(s -> {
                  io.stdoutConsumer().accept(s);
                  return s;
                })
                .filter(io.stdoutFilter())
        )
        .add(
            this.stderr()
                .map(s -> {
                  io.stderrConsumer().accept(s);
                  return s;
                })
                .filter(io.stderrFilter())
        )
        .withExecutorService(excutorService)
        .build();
  }

  private static int getPid(Process proc) {
    int ret;
    try {
      Field f = proc.getClass().getDeclaredField("pid");
      boolean accessible = f.isAccessible();
      f.setAccessible(true);
      try {
        ret = Integer.parseInt(f.get(proc).toString());
      } finally {
        f.setAccessible(accessible);
      }
    } catch (IllegalAccessException | NumberFormatException | SecurityException | NoSuchFieldException e) {
      throw new RuntimeException(String.format("PID isn't available on this platform. (%s)", e.getClass().getSimpleName()), e);
    }
    return ret;
  }

  public Selector<String> getSelector() {
    return selector;
  }
}
