package com.github.dakusui.streamablecmd.core;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class StreamableProcess extends Process {
  private final Process process;
  private final Charset charset;

  public StreamableProcess(Process process, Charset charset) {
    this.process = process;
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

  /**
   * Returns a {@code Stream<String>} object that represents standard output
   * of the underlying process.
   */
  public Stream<String> stdout() {
    return Utils.toStream(getInputStream(), this.charset);
  }

  /**
   * Returns a {@code Stream<String>} object that represents standard error
   * of the underlying process.
   */
  public Stream<String> stderr() {
    return Utils.toStream(getErrorStream(), this.charset);
  }

  /**
   * Returns a {@code Consumer<String>} object that represents standard input
   * of the underlying process.
   */
  public Consumer<String> stdin() {
    return Utils.toConsumer(this.getOutputStream(), this.charset);
  }

  public int getPid() {
    return getPid(this.process);
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
    } catch (IllegalAccessException e) {
      throw new RuntimeException("PID isn't available on this platform. (IllegalAccess) ", e);
    } catch (NoSuchFieldException e) {
      throw new RuntimeException("PID isn't available on this platform. (NoSuchField)", e);
    } catch (SecurityException e) {
      throw new RuntimeException("PID isn't available on this platform. (Security)", e);
    } catch (NumberFormatException e) {
      throw new RuntimeException("PID isn't available on this platform. (NumberFormat)", e);
    }
    return ret;
  }

}
