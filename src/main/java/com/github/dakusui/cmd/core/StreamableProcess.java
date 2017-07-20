package com.github.dakusui.cmd.core;

import com.github.dakusui.cmd.Shell;
import com.github.dakusui.cmd.exceptions.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

public class StreamableProcess extends Process {
  private static final Logger LOGGER = LoggerFactory.getLogger(StreamableProcess.class);
  private final Process          process;
  private final Stream<String>   stdout;
  private final Stream<String>   stderr;
  private final Consumer<String> stdin;
  private final Selector<String> selector;
  private final Config           config;
  private final String           command;
  private final Shell            shell;

  public StreamableProcess(Shell shell, String command, Config config) {
    this.process = createProcess(shell, command);
    this.config = requireNonNull(config);
    this.stdout = IoUtils.toStream(this.getInputStream(), config.charset());
    this.stderr = IoUtils.toStream(this.getErrorStream(), config.charset());
    this.stdin = IoUtils.flowControlValve(IoUtils.toConsumer(this.getOutputStream(), config.charset()), 100);
    this.selector = createSelector(config, this.stdin(), this.stdout(), this.stderr());
    this.shell = shell;
    this.command = command;
  }

  private static Process createProcess(Shell shell, String command) {
    try {
      return Runtime.getRuntime().exec(
          Stream.concat(
              Stream.concat(
                  Stream.of(shell.program()),
                  shell.options().stream()
              ),
              Stream.of(command)
          ).collect(toList()).toArray(new String[shell.options().size() + 2])
      );
    } catch (IOException e) {
      throw Exceptions.wrap(e);
    }
  }

  /**
   * from
   */
  @Override
  public OutputStream getOutputStream() {
    return new BufferedOutputStream(process.getOutputStream());
  }

  /**
   * to
   */
  @Override
  public InputStream getInputStream() {
    return new BufferedInputStream(process.getInputStream());
  }

  /**
   * stderr
   */
  @Override
  public InputStream getErrorStream() {
    return new BufferedInputStream(process.getErrorStream());
  }

  @Override
  public int waitFor() throws InterruptedException {
    LOGGER.debug("BEGIN:{}", this);
    try {
      return process.waitFor();
    } finally {
      LOGGER.debug("END:{}", this);
    }
  }

  @Override
  public int exitValue() {
    return process.exitValue();
  }

  @Override
  public void destroy() {
    LOGGER.debug("BEGIN:{}", this);
    process.destroy();
    LOGGER.debug("END:{}", this);
  }

  @Override
  public String toString() {
    return String.format("StreamableProcess:%s '%s'", this.shell, this.command);
  }

  public Stream<String> stream() {
    return this.getSelector().stream();
  }

  /**
   * Returns a {@code Stream<String>} object that represents standard output
   * of the underlying process.
   */
  private Stream<String> stdout() {
    return this.stdout;
  }

  /**
   * Returns a {@code Stream<String>} object that represents standard error
   * of the underlying process.
   */
  private Stream<String> stderr() {
    return this.stderr;
  }

  /**
   * Returns a {@code Consumer<String>} object that represents standard input
   * of the underlying process.
   */
  private Consumer<String> stdin() {
    return this.stdin;
  }

  public int getPid() {
    return getPid(this.process);
  }

  public Config getConfig() {
    return config;
  }

  private Selector<String> getSelector() {
    return selector;
  }

  private static Selector<String> createSelector(Config config, Consumer<String> stdin, Stream<String> stdout, Stream<String> stderr) {
    new Thread(() -> {
      config.stdin().forEach(IoUtils.flowControlValve(stdin, 100));
    }).start();
    return new Selector.Builder<String>(
        String.format("StreamableProcess:%s", Thread.currentThread().getId())
        //    ).add(
        //        config.stdin(),
        //        stdin,
        //        false
    ).add(
        config.stdoutTransformer().apply(stdout),
        config.stdoutConsumer(),
        true
    ).add(
        config.stderrTransformer().apply(stderr),
        config.stderrConsumer(),
        false
    ).build(
    );
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

  public static class Config {
    private Builder builder;

    Config(Builder builder) {
      this.builder = builder;
    }

    Stream<String> stdin() {
      return Stream.concat(
          builder.stdin,
          Stream.of((String) null)
      );
    }

    Consumer<String> stdoutConsumer() {
      return builder.stdoutConsumer;
    }

    Consumer<String> stderrConsumer() {
      return builder.stderrConsumer;
    }

    Function<Stream<String>, Stream<String>> stdoutTransformer() {
      return builder.stdoutTransformer;
    }

    Function<Stream<String>, Stream<String>> stderrTransformer() {
      return builder.stderrTransformer;
    }

    Charset charset() {
      return builder.charset;
    }

    public static Config.Builder builder() {
      return builder(Stream.empty());
    }

    public static Config.Builder builder(Stream<String> stdin) {
      return new Config.Builder().configureStdin(stdin);
    }

    public static class Builder {
      Stream<String>                           stdin;
      Consumer<String>                         stdoutConsumer;
      Function<Stream<String>, Stream<String>> stdoutTransformer;
      Consumer<String>                         stderrConsumer;
      Function<Stream<String>, Stream<String>> stderrTransformer;
      Charset                                  charset;

      public Builder() {
      }

      public Builder configureStdin(Stream<String> stdin) {
        this.stdin = requireNonNull(stdin);
        return this;
      }

      public Builder configureStdout(Consumer<String> consumer, Function<Stream<String>, Stream<String>> stdoutTransformer) {
        this.stdoutConsumer = requireNonNull(consumer);
        this.stdoutTransformer = requireNonNull(stdoutTransformer);
        return this;
      }

      public Builder configureStderr(Consumer<String> consumer, Function<Stream<String>, Stream<String>> stderrTransformer) {
        this.stderrConsumer = requireNonNull(consumer);
        this.stderrTransformer = requireNonNull(stderrTransformer);
        return this;
      }

      public Builder charset(Charset charset) {
        this.charset = requireNonNull(charset);
        return this;
      }

      public Config build() {
        return new Config(this);
      }

    }
  }
}
