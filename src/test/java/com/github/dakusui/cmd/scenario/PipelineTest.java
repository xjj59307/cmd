package com.github.dakusui.cmd.scenario;

import com.github.dakusui.cmd.Cmd;
import com.github.dakusui.cmd.Shell;
import com.github.dakusui.cmd.core.StreamableProcess;
import com.github.dakusui.cmd.utils.TestUtils;
import org.junit.Test;

import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class PipelineTest extends TestUtils.TestBase {
  @Test
  public void givenCommandPipeline$whenRunIt$thenAllDataProcessed() {
    List<String> out = new LinkedList<>();
    Cmd.cmd(
        Shell.local(),
        "echo hello && echo world"
    ).connect(
        "cat -n"
    ).connect(
        "sort -r"
    ).connect(
        "sed 's/hello/HELLO/'"
    ).connect(
        "sed -E 's/^ +//'"
    ).stream(
    ).map(
        s -> String.format("<%s>", s)
    ).forEach(
        out::add
    );
    assertEquals("<2\tworld>", out.get(0));
    assertEquals("<1\tHELLO>", out.get(1));
  }

  @Test
  public void givenCmd$whenGetProcessConfig$thenReturnedObjectSane() {
    Cmd cmd = Cmd.cmd(Shell.local(), "echo hello");
    StreamableProcess.Config processConfig = cmd.getProcessConfig();

    System.out.println(processConfig.charset());
    assertEquals(Charset.defaultCharset(), processConfig.charset());
  }

}
