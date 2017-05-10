package com.github.dakusui.cmd.streamable;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class StreamableProcess extends Process {
  private final Process          process;
  private final Consumer<String> stdoutConsumer;
  private final Consumer<String> stderrConsumer;
  private final Charset          charset;

  public StreamableProcess(Process process, Consumer<String> stdoutConsumer, Consumer<String> stderrConsumer, Charset charset) {
    this.process = process;
    this.stdoutConsumer = stdoutConsumer;
    this.stderrConsumer = stderrConsumer;
    this.charset = charset;
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
  }

  public Stream<String> stdout() {
    return Utils.toStream(getInputStream(), this.charset)
        .filter(s -> {
          stdoutConsumer.accept(s);
          return true;
        });
  }

  public Stream<String> stderr() {
    return Utils.toStream(getErrorStream(), this.charset)
        .filter(s -> {
          stderrConsumer.accept(s);
          return true;
        });
  }

  public Consumer<String> stdin() {
    return Utils.toConsumer(this.getOutputStream(), this.charset);
  }
}
