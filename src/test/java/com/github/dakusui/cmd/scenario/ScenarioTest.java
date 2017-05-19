package com.github.dakusui.cmd.scenario;

import com.github.dakusui.cmd.Cmd;
import com.github.dakusui.cmd.Shell;
import com.github.dakusui.cmd.exceptions.CommandExecutionException;
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
import java.util.function.Predicate;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;

@RunWith(JCUnit8.class)
public class ScenarioTest extends TestUtils.TestBase {

  private static final Consumer<String> NOP    = s -> {
  };
  private static final Consumer<String> STDOUT = System.out::println;
  private static final Consumer<String> STDERR = System.err::println;

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
    System.out.printf("shell='%s'%n", String.join(" ", shell.composeCommandLine()));
    System.out.printf("command='%s'%n", String.join(" ", command));
    System.out.printf("stdin='%s'%n", stdin);
    System.out.printf("stdoutConsumer='%s'(%s)%n", stdoutConsumer, redirectsStdout);
    System.out.printf("stderrConsumer='%s'(%s)%n", stderrConsumer, redirectsStderr);
  }

  @Test
  @Given("!exitsWithNonZero")
  public void whenRunCommand(
      @From("shell") Shell shell,
      @From("command") String[] command,
      @From("stdin") List<String> stdin,
      @From("stdoutConsumer") Consumer<String> stdoutConsumer,
      @From("redirectsStdout") boolean redirectsStdout,
      @From("stderrConsumer") Consumer<String> stderrConsumer,
      @From("redirectsStderr") boolean redirectsStderr
  ) {
    List<String> stdout = new LinkedList<>();
    Cmd.run(
        shell,
        Cmd.Io.builder(stdin.stream())
            .configureStdout(stdoutConsumer, redirectsStdout)
            .configureStderr(stderrConsumer, redirectsStderr)
            .build(),
        command
    ).forEach(
        ((Consumer<String>) System.out::println)
            .andThen(stdout::add)
    );
    assertThat(stdout, stdoutMatcher(stdin, String.join(" ", command), redirectsStdout));
  }

  @Test(expected = CommandExecutionException.class)
  @Given("exitsWithNonZero")
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
    try {
      Cmd.run(
          shell,
          Cmd.Io.builder(stdin.stream())
              .configureStdout(stdoutConsumer, redirectsStdout)
              .configureStderr(stderrConsumer, redirectsStderr)
              .build(),
          command
      ).forEach(
          ((Consumer<String>) System.out::println)
              .andThen(stdout::add)
      );
    } catch (CommandExecutionException e) {
      assertThat(stdout, stdoutMatcher(stdin, String.join(" ", command), redirectsStdout));
      throw e;
    }
  }

  @Condition
  public boolean exitsWithNonZero(
      @From("command") String[] command
  ) {
    return Arrays.stream(command).anyMatch(s -> s.contains("exit 1"));
  }

  private Predicate<List<String>> stdoutChecker(List<String> stdin, String command, boolean redirectsStdout) {
    return stdout -> {
      if (redirectsStdout) {
        if (Objects.equals(command, "cat")) {
          return stdin.equals(stdout);
        }
        if (command.contains("cat -n"))
          return "     1\tHello".equals(String.join("", stdout));
        return "Hello".equals(String.join("", stdout));
      }
      return emptyList().equals(stdout);
    };
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
