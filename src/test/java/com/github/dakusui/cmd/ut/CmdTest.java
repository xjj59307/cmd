package com.github.dakusui.cmd.ut;

import com.github.dakusui.cmd.utils.TestUtils;
import com.github.dakusui.cmd.Cmd;
import com.github.dakusui.cmd.Shell;
import com.github.dakusui.cmd.exceptions.CommandExecutionException;
import org.junit.Test;

import java.io.IOException;
import java.util.Date;
import java.util.stream.Stream;

public class CmdTest extends TestUtils.StdOutTestBase {
  private Cmd.Io defaultIo = createIo(Stream.empty());

  private Cmd.Io.Base createIo(Stream<String> stdin) {
    return new Cmd.Io.Base(stdin) {
      @Override
      public boolean redirectsStdout() {
        return true;
      }

      @Override
      public boolean redirectsStderr() {
        return false;
      }

      @Override
      protected void consumeStdout(String s) {
        System.out.println(new Date() + ":" + s);
      }

      @Override
      protected void consumeStderr(String s) {
        System.err.println(new Date() + ":" + s);
      }

      @Override
      protected boolean exitValue(int exitValue) {
        return exitValue == 0;
      }
    };
  }

  @Test(expected = CommandExecutionException.class)
  public void givenCommandExitWith1$whenRunLocally$thenCommandExecutionExceptionThrown() {
    Cmd.run(Shell.local(), "echo hello && exit 1").forEach(System.out::println);
  }

  @Test(expected = CommandExecutionException.class)
  public void main2() throws IOException {
    try {
      new Cmd.Builder()
          .withShell(new Shell.Builder.ForLocal().build())
          .add("echo $(which echo) && echo \"hello\" && cat hello")
          .configure(defaultIo)
          .build()
          .run()
          .forEach(System.out::println);
    } catch (CommandExecutionException e) {
      System.err.println(e.exitCode());
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
    } catch (CommandExecutionException e) {
      System.err.println("exitcode:" + e.exitCode());
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
    } catch (CommandExecutionException e) {
      System.err.println(e.exitCode());
      System.err.println(String.join(":", e.commandLine()));
      throw e;
    }
  }

  @Test
  public void main1() throws IOException {
    try {
      Cmd cmd = new Cmd.Builder()
          .withShell(new Shell.Builder.ForLocal().withProgram("sh").addOption("-c").build())
          .add(String.format("cat /dev/zero | head -c 100000 | %s 80", base64()))
          .configure(createIo(Stream.of("Hello", "world")))
          .build();
      System.out.println("commandLine=" + cmd.getShell());
      cmd.run()
          .forEach(System.out::println);
    } catch (CommandExecutionException e) {
      System.err.println(e.exitCode());
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
