package com.github.dakusui.cmd.scenario;

import com.github.dakusui.cmd.Cmd;
import com.github.dakusui.cmd.Shell;
import com.github.dakusui.cmd.core.StreamableProcess;
import com.github.dakusui.cmd.exceptions.UnexpectedExitValueException;
import com.github.dakusui.cmd.utils.TestUtils;
import com.github.dakusui.jcunit8.factorspace.Parameter;
import com.github.dakusui.jcunit8.runners.junit4.JCUnit8;
import com.github.dakusui.jcunit8.runners.junit4.annotations.Condition;
import com.github.dakusui.jcunit8.runners.junit4.annotations.From;
import com.github.dakusui.jcunit8.runners.junit4.annotations.Given;
import com.github.dakusui.jcunit8.runners.junit4.annotations.ParameterSource;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(JCUnit8.class)
public class ScenarioTest extends TestUtils.TestBase {

  private static final Consumer<String> NOP    = s -> {
  };
  /**
   * If you make this a method reference, you cannot suppress output to stdout
   * even if you are using TestUtils.TestBase.
   */
  @SuppressWarnings("Convert2MethodRef")
  private static final Consumer<String> STDOUT = x -> System.out.println(x);
  /**
   * If you make this a method reference, you cannot suppress output to stderr
   * even if you are using TestUtils.TestBase.
   */
  @SuppressWarnings("Convert2MethodRef")
  private static final Consumer<String> STDERR = x -> System.err.println(x);

  @ParameterSource
  public Parameter.Factory<Shell> shell() {
    return Parameter.Simple.Factory.of(asList(
        Shell.local(),
        Shell.ssh(TestUtils.userName(), "localhost", TestUtils.identity()),
        Shell.ssh(TestUtils.userName(), TestUtils.hostName(), TestUtils.identity())
    ));
  }

  @ParameterSource
  public Parameter.Factory<String[]> command() {
    return Parameter.Simple.Factory.of(asList(
        new String[] { "echo Hello" },
        new String[] { "echo", "Hello" },
        new String[] { "echo Hello | cat -n" },
        new String[] { "echo Hello && exit 1" },
        new String[] { "cat" }
    ));
  }

  @ParameterSource
  public Parameter.Factory<List<String>> stdin() {
    return Parameter.Simple.Factory.of(asList(
        emptyList(),
        singletonList("a"),
        asList("a", "b", "c")
    ));
  }

  @ParameterSource
  public Parameter.Factory<Consumer<String>> stdoutConsumer() {
    return Parameter.Simple.Factory.of(asList(
        NOP,
        STDOUT,
        STDERR
    ));
  }

  @ParameterSource
  public Parameter.Factory<Boolean> redirectsStdout() {
    return Parameter.Simple.Factory.of(asList(
        true,
        false
    ));
  }

  @ParameterSource
  public Parameter.Factory<Consumer<String>> stderrConsumer() {
    return Parameter.Simple.Factory.of(asList(
        NOP,
        STDOUT,
        STDERR
    ));
  }

  @ParameterSource
  public Parameter.Factory<Boolean> redirectsStderr() {
    return Parameter.Simple.Factory.of(asList(
        true,
        false
    ));
  }

  @Condition
  public boolean commandShouldExitWithZero(
      @From("command") String[] command
  ) {
    return Arrays.stream(command).noneMatch(s -> s.contains("exit 1"));
  }


  @Test
  public void print(
      @From("shell") Shell shell,
      @From("command") String[] command,
      @From("stdin") List<String> stdin,
      @From("stdoutConsumer") Consumer<String> stdoutConsumer,
      @From("redirectsStdout") boolean redirectsStdout,
      @From("stderrConsumer") Consumer<String> stderrConsumer,
      @From("redirectsStderr") boolean redirectsStderr
  ) {
    System.out.printf("shell='%s'%n", String.join(" ", shell.format()));
    System.out.printf("command='%s'%n", String.join(" ", command));
    System.out.printf("stdin='%s'%n", stdin);
    System.out.printf("stdoutConsumer='%s'(%s)%n", stdoutConsumer, redirectsStdout);
    System.out.printf("stderrConsumer='%s'(%s)%n", stderrConsumer, redirectsStderr);
  }

  @Test
  @Given("commandShouldExitWithZero")
  public void whenRunCommand$thenExpectedDataWrittenToStdout(
      @From("shell") Shell shell,
      @From("command") String[] command,
      @From("stdin") List<String> stdin,
      @From("stdoutConsumer") Consumer<String> stdoutConsumer,
      @From("redirectsStdout") boolean redirectsStdout,
      @From("stderrConsumer") Consumer<String> stderrConsumer,
      @From("redirectsStderr") boolean redirectsStderr
  ) {
    List<String> stdout = new LinkedList<>();
    String cmdLine = String.join(" ", command);
    Cmd.stream(
        shell,
        StreamableProcess.Config.builder(stdin.stream())
            .configureStdout(stdoutConsumer, s -> redirectsStdout)
            .configureStderr(stderrConsumer, s -> redirectsStderr)
            .build(),
        cmdLine
    ).forEach(
        ((Consumer<String>) System.out::println)
            .andThen(stdout::add)
    );
    assertThat(stdout, stdoutMatcher(stdin, cmdLine, redirectsStdout));
  }

  @Test(timeout = 5_000)
  @Given("commandShouldExitWithZero")
  public void whenRunCommandGetPidAndDestroy$thenNotBlocked(
      @From("shell") Shell shell,
      @From("command") String[] command,
      @From("stdin") List<String> stdin,
      @From("stdoutConsumer") Consumer<String> stdoutConsumer,
      @From("redirectsStdout") boolean redirectsStdout,
      @From("stderrConsumer") Consumer<String> stderrConsumer,
      @From("redirectsStderr") boolean redirectsStderr
  ) {
    Cmd cmd = buildCommand(
        shell,
        Stream.concat(
            Stream.of("sleep 0.2", "&&"),
            Arrays.stream(command)
        ).collect(toList()).toArray(new String[0]),
        stdin,
        stdoutConsumer,
        redirectsStdout,
        stderrConsumer,
        redirectsStderr
    );
    Stream<String> stream = cmd.stream();
    try {
      int pid = cmd.getPid();
      System.out.println("pid=" + pid);
      assertTrue("pid=" + pid, pid > 0);
    } finally {
      stream.forEach(System.out::println);
      cmd.destroy();
    }
  }

  @Test(expected = UnexpectedExitValueException.class)
  @Given("!commandShouldExitWithZero")
  public void whenRunCommand$thenCommandExecutionExceptionThrown(
      @From("shell") Shell shell,
      @From("command") String[] command,
      @From("stdin") List<String> stdin,
      @From("stdoutConsumer") Consumer<String> stdoutConsumer,
      @From("redirectsStdout") boolean redirectsStdout,
      @From("stderrConsumer") Consumer<String> stderrConsumer,
      @From("redirectsStderr") boolean redirectsStderr
  ) {
    List<String> stdout = new LinkedList<>();
    Cmd cmd = buildCommand(
        shell,
        command,
        stdin,
        stdoutConsumer,
        redirectsStdout,
        stderrConsumer,
        redirectsStderr
    );
    try {
      cmd.stream().forEach(
          ((Consumer<String>) System.out::println)
              .andThen(stdout::add)
      );
    } catch (UnexpectedExitValueException e) {
      assertThat(stdout, stdoutMatcher(stdin, String.join(" ", command), redirectsStdout));
      assertTrue(e.exitValue() != 0);
      assertTrue(e.exitValue() == cmd.exitValue());
      throw e;
    }
  }

  private Cmd buildCommand(
      Shell shell,
      String[] command,
      List<String> stdin,
      Consumer<String> stdoutConsumer,
      boolean redirectsStdout,
      Consumer<String> stderrConsumer,
      boolean redirectsStderr
  ) {
    return new Cmd.Builder()
        .withShell(shell)
        .configure(StreamableProcess.Config.builder(stdin.stream())
            .configureStdout(stdoutConsumer, s -> redirectsStdout)
            .configureStderr(stderrConsumer, s -> redirectsStderr).build())
        .addAll(asList(command))
        .build();
  }

  private Matcher<List<String>> stdoutMatcher(List<String> stdin, String command, boolean redirectsStdout) {
    return new BaseMatcher<List<String>>() {
      @Override
      public boolean matches(Object o) {
        return expectation(stdin, command, redirectsStdout).equals(o);
      }

      private List<String> expectation(List<String> stdin, String command, boolean redirectsStdout) {
        if (redirectsStdout) {
          if (Objects.equals(command, "cat")) {
            return stdin;
          }
          if (command.contains("cat -n"))
            return singletonList("     1\tHello");
          return singletonList("Hello");
        }
        return emptyList();
      }

      @Override
      public void describeTo(Description description) {
        description
            .appendText("expectation(")
            .appendText("stdin:").appendValue(stdin).appendText(",")
            .appendText("command:").appendValue(command).appendText(",")
            .appendText("redirectsStdout:").appendValue(redirectsStdout)
            .appendText(")=")
            .appendValue(expectation(stdin, command, redirectsStdout))

        ;
      }
    };
  }
}
