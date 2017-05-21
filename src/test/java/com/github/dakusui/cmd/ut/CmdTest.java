package com.github.dakusui.cmd.ut;

import com.github.dakusui.cmd.Cmd;
import com.github.dakusui.cmd.Shell;
import com.github.dakusui.cmd.core.StreamableProcess;
import com.github.dakusui.cmd.exceptions.UnexpectedExitValueException;
import com.github.dakusui.cmd.utils.TestUtils;
import org.junit.Test;

import java.io.IOException;
import java.util.Date;
import java.util.stream.Stream;

public class CmdTest extends TestUtils.TestBase {
  private StreamableProcess.Config defaultIo = createIo(Stream.empty());

  private StreamableProcess.Config createIo(Stream<String> stdin) {
    return new StreamableProcess.Config.Builder(stdin)
        .configureStdin(Stream.empty())
        .configureStdout(s -> System.out.println(new Date() + ":" + s))
        .configureStderr(s -> System.err.println(new Date() + ":" + s))
        .build();
  }

  @Test(timeout = 10_000)
  public void simplyEchoHello() {
    Cmd.run(Shell.local(), "echo hello").forEach(System.out::println);
  }

  @Test(expected = UnexpectedExitValueException.class)
  public void givenCommandExitWith1$whenRunLocally$thenCommandExecutionExceptionThrown() {
    Cmd.run(Shell.local(), "echo hello && exit 1").forEach(System.out::println);
  }

  @Test(expected = UnexpectedExitValueException.class)
  public void givenCommandExitWith$whenRunItLocallyTwice$thenCommandExecutionExceptionThrown() {
    String command = "echo hello && exit 1";
    Cmd.run(Shell.local(), StreamableProcess.Config.builder(Stream.empty()).build(), command).forEach(System.out::println);
    Cmd.run(Shell.local(), command).forEach(System.out::println);
  }

  @Test(expected = UnexpectedExitValueException.class)
  public void main2() throws IOException {
    try {
      new Cmd.Builder()
          .withShell(new Shell.Builder.ForLocal().build())
          .add("echo $(which echo) && echo \"hello\" && cat hello")
          .configure(defaultIo)
          .build()
          .run()
          .forEach(System.out::println);
    } catch (UnexpectedExitValueException e) {
      System.err.println(e.exitValue());
      System.err.println(String.join(":", e.commandLine()));
      throw e;
    }
  }

  @Test
  public void main() throws IOException {
    try {
      new Cmd.Builder()
          .withShell(
              new Shell.Builder.ForSsh("localhost")
                  .userName(TestUtils.userName())
                  .identity(TestUtils.identity())
                  .build()
          )
          .add("echo")
          .add("hello")
          .configure(defaultIo)
          .build()
          .run()
          .forEach(System.out::println);
    } catch (UnexpectedExitValueException e) {
      System.err.println("exitcode:" + e.exitValue());
      System.err.println("commandline:" + String.join(" ", e.commandLine()));
      throw e;
    }
  }

  @Test(timeout = 15_000)
  public void main4() throws IOException {
    try {
      new Cmd.Builder()
          .withShell(new Shell.Builder.ForLocal().build())
          .configure(createIo(Stream.of("hello", "world", "everyone")))
          .add("cat -n")
          .build()
          .run()
          .forEach(System.out::println);
    } catch (UnexpectedExitValueException e) {
      System.err.println(e.exitValue());
      System.err.println(String.join(":", e.commandLine()));
      throw e;
    }
  }

  @Test
  public void main1() throws IOException {
    try {
      Cmd cmd = new Cmd.Builder()
          .withShell(new Shell.Builder.ForLocal().withProgram("sh").clearOptions().addOption("-c").build())
          .add(String.format("cat /dev/zero | head -c 100000 | %s 80", base64()))
          .configure(createIo(Stream.of("Hello", "world")))
          .build();
      System.out.println("commandLine=" + cmd);
      cmd.run()
          .forEach(System.out::println);
    } catch (UnexpectedExitValueException e) {
      System.err.println(e.exitValue());
      System.err.println(String.join(":", e.commandLine()));
      throw e;
    }
  }

  private static String base64() {
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

  private static String systemName() {
    return System.getProperty("os.name");
  }

}
