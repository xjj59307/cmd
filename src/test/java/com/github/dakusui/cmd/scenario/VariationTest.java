package com.github.dakusui.cmd.scenario;

import com.github.dakusui.cmd.Cmd;
import com.github.dakusui.cmd.Shell;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class VariationTest {
  @Test
  public void givenPipedCmds$whenRun$thenPiped() {
    Cmd.run(
        Shell.local(),
        Cmd.connect(
            Cmd.run(
                Shell.local(),
                "echo hello && echo world && echo HELLO && echo WORLD"
            )),
        "cat -n"
    ).forEach(Cmd.cmd(Shell.local(), "sort | cat -n").andThen(System.out::println));
  }
}
