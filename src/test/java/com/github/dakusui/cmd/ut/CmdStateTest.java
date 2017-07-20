package com.github.dakusui.cmd.ut;

import com.github.dakusui.cmd.Cmd;
import com.github.dakusui.cmd.Shell;
import com.github.dakusui.cmd.utils.TestUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

public class CmdStateTest extends TestUtils.TestBase {
  @Test(expected = IllegalStateException.class)
  public void givenCmdNotStarted$whenExitValue$thenIllegalStateWillBeThrown() {
    Cmd cmd = Cmd.cmd(Shell.local(), "echo hello");
    try {
      cmd.getStreamableProcess().exitValue();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage(), CoreMatchers.containsString("Current state=<PREPARING>"));
      throw e;
    }
  }

  @Test(expected = IllegalStateException.class)
  public void givenCmdNotStarted$whenDestroy$thenIllegalStateWillBeThrown() {
    Cmd cmd = Cmd.cmd(Shell.local(), "echo hello");
    try {
      cmd.abort();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage(), CoreMatchers.containsString("Current state=<PREPARING>"));
      throw e;
    }
  }

  @Test(expected = IllegalStateException.class)
  public void givenCmdNotStarted$whenGetPid$thenIllegalStateWillBeThrown() {
    Cmd cmd = Cmd.cmd(Shell.local(), "echo hello");
    try {
      cmd.getStreamableProcess().getPid();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage(), CoreMatchers.containsString("Current state=<PREPARING>"));
      throw e;
    }
  }

  @Test(expected = IllegalStateException.class)
  public void givenCmdAlreadyRun$whenRunAgain$thenIllegalStateWillBeThrown() {
    Cmd cmd = Cmd.cmd(Shell.local(), "echo hello");
    cmd.stream().forEach(System.out::println);
    try {
      cmd.stream();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage(), CoreMatchers.containsString("Current state=<CLOSED>"));
      throw e;
    }
  }

  @Test
  public void givenCmdAlreadyRun$whenGetState$thenIllegalStateWillBeThrown() {
    Cmd cmd = Cmd.cmd(Shell.local(), "echo hello");
    cmd.stream().forEach(System.out::println);

    assertEquals(Cmd.State.CLOSED, cmd.getState());
  }

  @Test
  public void givenCmdAlreadyRun$whenClose$thenIllegalStateWillBeThrown() {
    Cmd cmd = Cmd.cmd(Shell.local(), "echo hello");
    Stream<String> s = cmd.stream();
    System.out.println(s);
    cmd.close();
    assertEquals(Cmd.State.CLOSED, cmd.getState());
  }
}
