package com.github.dakusui.streamablecmd;

import com.github.dakusui.streamablecmd.core.Selector;
import com.github.dakusui.streamablecmd.core.StreamableProcess;
import com.github.dakusui.streamablecmd.exceptions.CommandExecutionException;
import com.github.dakusui.streamablecmd.exceptions.Exceptions;
import com.github.dakusui.streamablecmd.exceptions.Exceptions.Arguments;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

public interface Cmd {
  Stream<String> run();

  List<String> getCommandLine();

  class Builder {
    Shell shell;
    List<String> command = new LinkedList<>();
    Charset      charset = Charset.defaultCharset();
    Io config;


    public Builder add(String arg) {
      this.command.add(arg);
      return this;
    }

    public Builder withShell(Shell shell) {
      this.shell = shell;
      return this;
    }

    public Builder configure(Io config) {
      this.config = config;
      return this;
    }

    public Cmd build() {
      return new Impl(
          composeCommandLine(),
          charset,
          config);
    }

    private String[] composeCommandLine() {
      return this.shell.buildCommandLine(String.join(" ", this.command));
    }
  }

  class Impl implements Cmd {
    private static final Predicate<Object> SUPPRESS = new Predicate<Object>() {
      @Override
      public boolean test(Object o) {
        return o == SENTINEL;
      }
    };
    private static final Object            SENTINEL = new Object();

    private final String[] commandLine;
    private final Charset  charset;
    private final Io       io;

    Impl(String[] commandLine, Charset charset, Io io) {
      this.commandLine = commandLine;
      this.charset = charset;
      this.io = io;
    }

    private StreamableProcess startProcess() {
      return new StreamableProcess(
          createProcess(this.commandLine),
          charset);
    }

    @Override
    public Stream<String> run() {
      StreamableProcess process = startProcess();
      ExecutorService excutorService = Executors.newFixedThreadPool(3);
      return Stream.concat(
          new Selector.Builder<String>()
              .add(this.io.stdin(), process.stdin())
              .add(
                  process.stdout()
                      .map(s -> {
                        this.io.stdoutConsumer().accept(s);
                        return s;
                      })
                      .filter(SUPPRESS.or(s -> io.redirectsStdout()))
              )
              .add(
                  process.stderr()
                      .map(s -> {
                        this.io.stderrConsumer().accept(s);
                        return s;
                      })
                      .filter(SUPPRESS.or(s -> io.redirectsStderr()))
              )
              .withExecutorService(excutorService)
              .build()
              .select(),
          Stream.of(SENTINEL)
      ).filter(
          o -> {
            if (o == SENTINEL) {
              try {
                try {
                  try {
                    try {
                      int exitValue = waitFor(process);
                      if (!(this.io.exitValue().test(exitValue))) {
                        throw new CommandExecutionException(exitValue, this.getCommandLine().toArray(new String[0]), process.getPid());
                      }
                      return false;
                    } finally {
                      process.stdout().close();
                    }
                  } finally {
                    process.stderr().close();
                  }
                } finally {
                  process.stdin().accept(null);
                }
              } finally {
                excutorService.shutdown();
              }
            }
            return true;
          }
      ).map(
          o -> (String) o
      );
    }

    int waitFor(Process process) {
      try {
        return process.waitFor();
      } catch (InterruptedException e) {
        throw Exceptions.wrap(e);
      }
    }

    @Override
    public List<String> getCommandLine() {
      return asList(commandLine);
    }

    private Process createProcess(String[] commandLine) {
      try {
        return Runtime.getRuntime().exec(commandLine);
      } catch (IOException e) {
        throw Exceptions.wrap(e);
      }
    }
  }

  interface Io {
    Io DEFAULT = new Impl(Stream.empty());

    Stream<String> stdin();

    Consumer<String> stdoutConsumer();

    Consumer<String> stderrConsumer();

    boolean redirectsStdout();

    boolean redirectsStderr();

    IntPredicate exitValue();

    abstract class Base implements Io {
      private final Stream<String> stdin;
      private Consumer<String> stdoutConsumer    = this::consumeStdout;
      private Consumer<String> stderrConsumer    = this::consumeStderr;
      private IntPredicate     exitValueConsumer = this::exitValue;

      protected Base(Stream<String> stdin) {
        this.stdin = Exceptions.Arguments.requireNonNull(stdin);
      }

      @Override
      public Stream<String> stdin() {
        return this.stdin;
      }

      @Override
      public Consumer<String> stdoutConsumer() {
        return stdoutConsumer;
      }

      @Override
      public Consumer<String> stderrConsumer() {
        return stderrConsumer;
      }

      @Override
      public IntPredicate exitValue() {
        return this.exitValueConsumer;
      }

      abstract protected void consumeStdout(String s);

      abstract protected void consumeStderr(String s);

      abstract protected boolean exitValue(int exitValue);
    }

    class Impl extends Base {
      public Impl(Stream<String> stdin) {
        super(stdin);
      }

      @Override
      protected void consumeStdout(String s) {
      }

      @Override
      protected void consumeStderr(String s) {
      }

      @Override
      protected boolean exitValue(int exitValue) {
        return exitValue == 0;
      }

      @Override
      public boolean redirectsStdout() {
        return true;
      }

      @Override
      public boolean redirectsStderr() {
        return false;
      }
    }
  }

  interface Shell {
    class Impl implements Shell {
      private final String       program;
      private final List<String> options;

      Impl(String program, List<String> options) {
        this.program = program;
        this.options = options;
      }

      String program() {
        return program;
      }

      List<String> options() {
        return options;
      }

      public String[] buildCommandLine(String command) {
        return Stream.concat(
            Stream.concat(
                Stream.of(program()),
                options().stream()
            ),
            Stream.of(command)
        ).collect(toList()).toArray(new String[0]);
      }
    }

    String[] buildCommandLine(String command);

    abstract class Builder<B extends Builder> {
      private String program;
      private List<String> options = new LinkedList<>();

      @SuppressWarnings("unchecked")
      public B withProgram(String program) {
        this.program = Exceptions.Arguments.requireNonNull(program);
        return (B) this;
      }

      @SuppressWarnings("unchecked")
      public B clearOptions() {
        this.options.clear();
        return (B) this;
      }

      @SuppressWarnings("unchecked")
      public B addOption(String option) {
        this.options.add(option);
        return (B) this;
      }

      @SuppressWarnings("unchecked")
      public B addOption(String option, String value) {
        this.options.add(option);
        this.options.add(value);
        return (B) this;
      }

      String getProgram() {
        return this.program;
      }

      List<String> getOptions() {
        return this.options;
      }

      public Shell build() {
        Arguments.requireNonNull(this.program);
        return new Shell.Impl(getProgram(), this.getOptions());
      }

      @SuppressWarnings("unchecked")
      public B addAllOptions(List<String> options) {
        options.forEach(this::addOption);
        return (B) this;
      }

      public static class ForLocal extends Builder<ForLocal> {
        public ForLocal() {
          this.withProgram("sh")
              .addOption("-c");
        }
      }

      public static class ForSsh extends Builder<ForSsh> {
        private       String userName;
        private final String hostName;
        private String identity = null;

        public ForSsh(String hostName) {
          this.hostName = Arguments.requireNonNull(hostName);
          this.withProgram("ssh")
              .addOption("-o", "PasswordAuthentication=no")
              .addOption("-o", "StrictHostkeyChecking=no");
        }

        public ForSsh userName(String userName) {
          this.userName = userName;
          return this;
        }

        public ForSsh identity(String identity) {
          this.identity = identity;
          return this;
        }

        List<String> getOptions() {
          return Stream.concat(
              super.getOptions().stream(),
              Stream.concat(
                  composeIdentity().stream(),
                  Stream.of(
                      composeAccount()
                  )
              )
          ).collect(toList());
        }

        List<String> composeIdentity() {
          return identity == null ?
              Collections.emptyList() :
              asList("-i", identity);
        }

        String composeAccount() {
          return userName == null ?
              hostName :
              String.format("%s@%s", userName, hostName);
        }
      }
    }
  }
}
