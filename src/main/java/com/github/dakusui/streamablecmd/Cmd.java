package com.github.dakusui.streamablecmd;

import com.github.dakusui.streamablecmd.core.StreamableProcess;
import com.github.dakusui.streamablecmd.core.Utils;
import com.github.dakusui.streamablecmd.exceptions.CommandExecutionException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.IntPredicate;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

public interface Cmd {
  Stream<String> run();

  List<String> getCommandLine();

  class Impl implements Cmd {
    private static final Object sentinel = new Object();

    private final IntPredicate     exitCodeChecker;
    private final String[]         commandLine;
    private final Charset          charset;
    private final File             stderrLogFile;
    private final Consumer<String> stdoutConsumer;
    private final Consumer<String> stderrConsumer;
    private final Stream<String>   stdinStream;

    private Impl(String[] commandLine, File stderrLogFile, IntPredicate exitCodeChecker, Charset charset, Consumer<String> stdoutConsumer, Consumer<String> stderrConsumer, Stream<String> stdinStream) {
      this.commandLine = commandLine;
      this.stderrLogFile = stderrLogFile;
      this.exitCodeChecker = exitCodeChecker;
      this.charset = charset;
      this.stdoutConsumer = stdoutConsumer;
      this.stderrConsumer = stderrConsumer;
      this.stdinStream = stdinStream;
    }

    @Override
    public Stream<String> run() {
      StreamableProcess process = new StreamableProcess(
          createProcess(this.commandLine),
          charset);
      drain(this.stdinStream, process.stdin());
      Stream<String> stdout = process.stdout();
      return Stream.concat(
          stdout,
          Stream.of(sentinel)
      ).filter(
          o -> {
            try {
              if (o == sentinel) {
                try (Stream<String> stderr = drain(process.stderr(), stderrLogFile, charset, stderrConsumer)) {
                  try {
                    int exitCode = process.waitFor();
                    if (!(exitCodeChecker.test(exitCode))) {
                      throw new CommandExecutionException(exitCode, commandLine, process.getPid(), stderr);
                    }
                    return false;
                  } finally {
                    stdout.close();
                  }
                }
              }
              return true;
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }
          }
      ).map(
          o -> (String) o
      ).filter(
          s -> {
            stdoutConsumer.accept(s);
            return true;
          }
      );
    }

    @Override
    public List<String> getCommandLine() {
      return asList(commandLine);
    }

    private void drain(Stream<String> source, Consumer<String> consumer) {
      new Thread(() -> {
        try {
          try {
            source.forEach(consumer);
          } finally {
            source.close();
          }
        } finally {
          consumer.accept(null);
        }
      }).start();
    }

    private Process createProcess(String[] commandLine) {
      try {
        return Runtime.getRuntime().exec(commandLine);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    @SuppressWarnings("EmptyCatchBlock")
    private static Stream<String> drain(Stream<String> stream, File file, Charset charset, Consumer<String> consumer) {
      AtomicBoolean monitor = new AtomicBoolean(false);
      new Thread(() -> {
        synchronized (monitor) {
          monitor.set(true);
          monitor.notify();
          try {
            stream.forEach(Utils.toConsumer(Utils.openForWrite(file), charset).andThen(consumer));
          } finally {
            stream.close();
          }
        }
      }).start();
      synchronized (monitor) {
        while (!monitor.get())
          try {
            monitor.wait();
          } catch (InterruptedException e) {
          }
      }
      return Utils.toStream(synchronize(Utils.openForRead(file), monitor), charset);
    }

    private static InputStream synchronize(InputStream is, Object monitor) {
      return new InputStream() {
        @Override
        public int read() throws IOException {
          synchronized (monitor) {
            return is.read();
          }
        }
      };
    }

  }

  class SshOptions {
    public final String hostName;
    public final String userName;
    public final File   identity;

    private SshOptions(String userName, String hostName, File identity) {
      this.userName = userName;
      this.hostName = hostName;
      this.identity = identity;
    }

    public static class Builder {
      String hostName = null;
      String userName = null;
      private File identity = null;

      public Builder setIdentity(File identity) {
        this.identity = identity;
        return this;
      }

      public Builder setUserName(String userName) {
        this.userName = userName;
        return this;
      }

      public Builder setHostName(String hostName) {
        this.hostName = hostName;
        return this;
      }

      public SshOptions build() {
        return new SshOptions(this.userName, this.hostName, this.identity);
      }
    }
  }

  class Builder {
    SshOptions sshOptions;
    String           program         = null;
    List<String>     arguments       = new LinkedList<>();
    Charset          charset         = Charset.defaultCharset();
    File             stderrLogFile   = null;
    IntPredicate     exitCodeChecker = value -> value == 0;
    Consumer<String> stdoutConsumer  = s -> {
    };
    Consumer<String> stderrConsumer  = s -> {
    };
    Stream<String>   stdinStream     = Stream.empty();

    public Builder setProgram(String command) {
      this.program = command;
      return this;
    }

    public Builder clearArguments() {
      arguments.clear();
      return this;
    }

    public Builder addArgument(String arg) {
      this.arguments.add(arg);
      return this;
    }

    public Builder addAllArguments(List<String> arguments) {
      arguments.forEach(this::addArgument);
      return this;
    }

    public Builder ssh(String userName, String hostName) {
      this.ssh(
          new SshOptions.Builder()
              .setUserName(userName)
              .setHostName(hostName)
              .build()
      );
      return this;
    }

    public Builder ssh(SshOptions options) {
      this.sshOptions = options;
      return this;
    }

    public Builder local() {
      this.sshOptions = null;
      return this;
    }

    public Builder setStderrLogFile(File stderrLogFile) {
      this.stderrLogFile = stderrLogFile;
      return this;
    }

    public Builder setStdoutConsumer(Consumer<String> stdoutConsumer) {
      this.stdoutConsumer = stdoutConsumer;
      return this;
    }

    public Builder setStderrConsumer(Consumer<String> stderrConsumer) {
      this.stderrConsumer = stderrConsumer;
      return this;
    }

    public Builder setStdinStream(Stream<String> stdinStream) {
      this.stdinStream = stdinStream;
      return this;
    }

    public Builder setExitCodeChecker(IntPredicate exitCodeChecker) {
      this.exitCodeChecker = exitCodeChecker;
      return this;
    }

    public Cmd build() {
      return new Impl(
          composeCommandLine(),
          this.stderrLogFile != null ?
              this.stderrLogFile :
              createTempFile(),
          exitCodeChecker,
          charset,
          stdoutConsumer,
          stderrConsumer,
          stdinStream);
    }

    private File createTempFile() {
      try {
        File ret = File.createTempFile("commandrunner-streamable-", ".log");
        ret.deleteOnExit();
        return ret;
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    private String[] composeCommandLine() {
      if (this.sshOptions == null) {
        return Stream.concat(
            Stream.of(this.program),
            this.arguments.stream())
            .collect(toList())
            .toArray(new String[this.arguments.size() + 1]);
      }
      return new LinkedList<String>() {{
        add("ssh");
        add("-o");
        add("PasswordAuthentication=no");
        add("-o");
        add("StrictHostKeyChecking=no");
        if (sshOptions.identity != null) {
          add("-i");
          add(sshOptions.identity.getAbsolutePath());
        }
        add(format("%s@%s", sshOptions.userName, sshOptions.hostName));
        add(format(
            "%s %s",
            program,
            String.join(" ", arguments)
        ));
      }}.toArray(new String[0]);
    }
  }

  static void main2(String... args) throws IOException {
    try {
      new Cmd.Builder()
          .setProgram("sh")
          .addArgument("-c")
          .addArgument("echo $(which echo) && echo \"hello\" && cat hello")
          .setStderrConsumer(s -> System.err.println(new Date() + ":" + s))
          .setStdoutConsumer(s -> System.out.println(new Date() + ":" + s))
          .build()
          .run()
          .forEach(System.out::println);
    } catch (CommandExecutionException e) {
      System.err.println(e.exitCode());
      System.err.println(String.join(":", e.commandLine()));
      e.stderr().forEach(System.err::println);
      throw e;
    }
  }

  static void main3(String... args) throws IOException {
    try {
      new Cmd.Builder()
          .ssh(new SshOptions.Builder()
              .setUserName("hiroshi.ukai")
              .setHostName("localhost")
              .setIdentity(new File("/Users/hiroshi.ukai/.ssh/id_rsa.p25283"))
              .build())
          .setProgram("echo")
          .addArgument("hello")
          .setStderrConsumer(s -> System.err.println(new Date() + ":" + s))
          .setStdoutConsumer(s -> System.out.println(new Date() + ":" + s))
          .build()
          .run()
          .forEach(System.out::println);
    } catch (CommandExecutionException e) {
      System.err.println("exitcode:" + e.exitCode());
      System.err.println("commandline:" + String.join(" ", e.commandLine()));
      e.stderr().forEach(System.err::println);
      throw e;
    }
  }

  static void main4(String... args) throws IOException {
    try {
      new Cmd.Builder()
          .setStdinStream(Stream.of("hello", "world", "everyone"))
          .setProgram("cat")
          .addArgument("-n")
          .setStderrConsumer(s -> System.err.println(new Date() + ":" + s))
          .setStdoutConsumer(s -> System.out.println(new Date() + ":" + s))
          .build()
          .run()
          .forEach(System.out::println);
    } catch (CommandExecutionException e) {
      System.err.println(e.exitCode());
      System.err.println(String.join(":", e.commandLine()));
      e.stderr().forEach(System.err::println);
      throw e;
    }
  }

  static void main(String... args) throws IOException {
    try {
      Cmd cmd;
      cmd = new Cmd.Builder()
          .setStdinStream(Stream.empty())
          .setProgram("sh")
          .addArgument("-c")
          .setStdinStream(Stream.of("Hello", "world"))
          .addArgument(format("cat /dev/zero | head -c 100000 | %s 80", base64()))
          .setStderrConsumer(s -> System.err.println(new Date() + ":" + s))
          .setStdoutConsumer(s -> System.out.println(new Date() + ":" + s))
          .build();
      System.out.println("commandLine=" + cmd.getCommandLine());
      cmd.run()
          .forEach(System.out::println);
    } catch (CommandExecutionException e) {
      System.err.println(e.exitCode());
      System.err.println(String.join(":", e.commandLine()));
      e.stderr().forEach(System.err::println);
      throw e;
    }
  }

  public static String base64() {
    String systemName = systemName();
    String ret;
    if ("Linux".equals(systemName)) {
      ret = "base64 -w";
    } else if ("Mac OS X".equals(systemName)) {
      ret = "base64 -b";
    } else {
      throw new RuntimeException(String.format("%s is not a supported platform.", systemName));
    }
    return ret;
  }

  static String systemName() {
    return System.getProperty("os.name");
  }

}
