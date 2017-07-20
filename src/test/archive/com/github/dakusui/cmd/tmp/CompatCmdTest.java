package com.github.dakusui.cmd.tmp;

import com.github.dakusui.cmd.Shell;
import com.github.dakusui.cmd.core.StreamableProcess;
import com.github.dakusui.cmd.exceptions.UnexpectedExitValueException;
import com.github.dakusui.cmd.utils.TestUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.stream.Stream;

public class CompatCmdTest extends TestUtils.TestBase {
  //  private StreamableProcess.Config defaultConfig = createConfig(Stream.empty());
/*
  private StreamableProcess.Config createConfig(Stream<String> stdin) {
    return new StreamableProcess.Config.Builder().configureStdin(Stream.empty())
        .configureStdin(stdin)
        .configureStdout(s -> System.out.println(new Date() + ":" + s))
        .configureStderr(s -> System.err.println(new Date() + ":" + s))
        .build();
  }
  */

  @Before
  public void before() throws InterruptedException {
    Thread.sleep(500);
  }

  @Test(timeout = 10_000)
  public void simplyEchoHello() {
    CompatCmd.stream(Shell.local(), "echo hello").forEach(System.out::println);
  }

  @Test(expected = UnexpectedExitValueException.class)
  public void givenCommandExitWith1$whenRunLocally$thenCommandExecutionExceptionThrown() {
    CompatCmd.stream(Shell.local(), "echo hello && exit 1").forEach(System.out::println);
  }

  @Test(expected = UnexpectedExitValueException.class)
  public void givenCommandExitWith$whenRunItLocallyTwice$thenCommandExecutionExceptionThrown() {
    String command = "echo hello && exit 1";
    CompatCmd.stream(Shell.local(), StreamableProcess.Config.builder(Stream.empty()).build(), command).forEach(System.out::println);
    CompatCmd.stream(Shell.local(), command).forEach(System.out::println);
  }

  /*
  @Test(expected = UnexpectedExitValueException.class)
  public void givenPipedCommandThatShouldFail$whenRunLocally$thenThrowsException() throws IOException {
    try {
      new CompatCmd.Builder()
          .withShell(new Shell.Builder.ForLocal().build())
          .add("echo $(which echo) && echo \"hello\" && cat hello")
          .configure(defaultConfig)
          .build()
          .stream()
          .forEach(System.out::println);
    } catch (UnexpectedExitValueException e) {
      System.err.println(e.exitValue());
      System.err.println(String.join(":", e.commandLine()));
      throw e;
    }
  }
  */

  /*
  @Test(timeout = 15_000)
  public void givenEchoHello$whenRunOverSshOnLocalhost$thenFinishesWithoutError() throws IOException {
    try {
      new CompatCmd.Builder()
          .withShell(
              new Shell.Builder.ForSsh("localhost")
                  .userName(TestUtils.userName())
                  .identity(TestUtils.identity())
                  .build()
          )
          .add("echo")
          .add("hello")
          .configure(defaultConfig)
          .build()
          .stream()
          .forEach(System.out::println);
    } catch (UnexpectedExitValueException e) {
      System.err.println("exitcode:" + e.exitValue());
      System.err.println("commandline:" + String.join(" ", e.commandLine()));
      throw e;
    }
  }
  */

  /*

  @Test(timeout = 15_000)
  public void given_hello_world_everyone$whenPipedToCat$thenPassedToDownstream() throws IOException {
    try {
      List<String> out = new LinkedList<>();
      new CompatCmd.Builder()
          .withShell(new Shell.Builder.ForLocal().build())
          .configure(createConfig(Stream.of("hello", "world", "everyone")))
          .add("cat -n")
          .build()
          .stream()
          .forEach(out::add);
      assertThat(out,
          allOf(
              TestUtils.<List<String>, Integer>matcherBuilder().transform("size", List::size).check("==3", i -> i.equals(3)).build(),
              TestUtils.<List<String>, String>matcherBuilder().transform("[0]", i -> i.get(0)).check("endsWith('hello')", v -> v.endsWith("hello")).build(),
              TestUtils.<List<String>, String>matcherBuilder().transform("[1]", i -> i.get(1)).check("endsWith('world')", v -> v.endsWith("world")).build(),
              TestUtils.<List<String>, String>matcherBuilder().transform("[2]", i -> i.get(2)).check("endsWith('everyone')", v -> v.endsWith("everyone")).build()
          ));
    } catch (UnexpectedExitValueException e) {
      System.err.println(e.exitValue());
      System.err.println(String.join(":", e.commandLine()));
      throw e;
    }
  }
  */
  /*

  @Test(timeout = 15_000)
  public void givenPipedCommandThatHandles10KB$whenRunWithCustomInput$thenFinishes() throws IOException {
    try {
      CompatCmd cmd = new CompatCmd.Builder()
          .withShell(new Shell.Builder.ForLocal().withProgram("sh").clearOptions().addOption("-c").build())
          .add(String.format("cat /dev/zero | head -c 100000 | %s 80", TestUtils.base64()))
          .configure(createConfig(Stream.of("Hello", "world")))
          .build();
      System.out.println("commandLine=" + cmd);
      cmd.stream()
          .forEach(System.out::println);
    } catch (UnexpectedExitValueException e) {
      System.err.println(e.exitValue());
      System.err.println(String.join(":", e.commandLine()));
      throw e;
    }
  }
*/
}
