package com.github.dakusui.cmd.ut;

import com.github.dakusui.cmd.CommandResult;
import com.github.dakusui.cmd.exceptions.CommandTimeoutException;
import com.github.dakusui.cmd.utils.TestUtils;
import junit.framework.TestCase;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.dakusui.cmd.CommandUtils.runLocal;
import static com.github.dakusui.cmd.CommandUtils.runRemote;
import static com.github.dakusui.cmd.utils.TestUtils.*;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.junit.Assert.assertThat;

public class CommandUtilsTest {
  private static Logger LOGGER = LoggerFactory.getLogger(CommandUtilsTest.class);

  @Test(timeout = 10_000)
  public void givenEchoHell$whenRun$thenHelloIsWrittenToStdout() throws Exception {
    assertCommandResult(
        runLocal("echo hello"), "hello",
        "",
        "hello",
        0
    );
  }

  @Test(timeout = 10_000)
  public void given2connectedEchoCommands$runLocally$thenOutputIsCorrect() throws Exception {
    assertCommandResult(
        runLocal("echo hello && echo hello"),
        "hello\nhello",
        "",
        "hello\nhello",
        0
    );
  }

  @Test(timeout = 10_000)
  public void givenEchoCommand$runTwiceLocally$thenTheyDoNotBlockAndEachOutputIsCorrect() throws Exception {
    LOGGER.info("stage - 1");
    assertCommandResult(
        runLocal("echo hello"),
        "hello",
        "",
        "hello",
        0
    );
    LOGGER.info("stage - 2");
    assertCommandResult(
        runLocal("echo hello"),
        "hello",
        "",
        "hello",
        0
    );
  }

  @Test(timeout = 10_000)
  public void givenFailingCommand$whenRunLocally$thenStderrIsWrittenToCommandResult() throws Exception {
    assertCommandResult(
        runLocal("cat NNN"), "",
        "cat: NNN: No such file or directory",
        "cat: NNN: No such file or directory",
        1
        // non existing file "NNN"
    );
  }

  @Test(timeout = 10_000)
  public void givenEchoCommandRedirectingStdoutToStderr$runLocally$thenStdErrIsWrittenToCommandResult() throws Exception {
    LOGGER.info("test_03");
    assertCommandResult(
        runLocal("echo WORLD >&2"), "",
        "WORLD",
        "WORLD",
        0
    );
  }

  @Test(timeout = 10_000)
  public void givenEchoCommandAfter1secSleep$whenRunLocally$thenOutputIsCorrect() throws Exception {
    LOGGER.info("test-07");

    assertCommandResult(
        runLocal("sleep 1 && echo hi"),
        "hi",
        "",
        "hi",
        0
    );
  }


  @Test(timeout = 10_000)
  public void given100echoConcatenatedCommands$whenRun$then100LinesWrittenCorrectly() throws Exception {
    LOGGER.info("test-08");
    StringBuilder cmd = new StringBuilder();
    for (int i = 100; i > 0; i--) {
      cmd.append("echo ").append(i);
      if (i != 1) {
        cmd.append(" && ");
      }
    }

    StringBuilder expected = new StringBuilder();
    for (int i = 100; i > 0; i--) {
      expected.append(i);
      if (i != 1) {
        expected.append(System.getProperty("line.separator"));
      }
    }

    assertCommandResult(
        runLocal(cmd.toString()),
        expected.toString(),
        "",
        expected.toString(),
        0
    );
  }

  /**
   * This test makes sure if ring buffer is working as expected.
   */
  @Test(timeout = 5_000)
  public void given101echoConcatenatedCommands$whenRun$then100LinesWrittenCorrectly() throws Exception {
    LOGGER.info("test-09");
    StringBuilder cmd = new StringBuilder();
    for (int i = 101; i > 0; i--) {
      cmd.append("echo ").append(i);
      if (i != 1) {
        cmd.append(" && ");
      }
    }

    StringBuilder expected = new StringBuilder();
    for (int i = 100; i > 0; i--) {
      expected.append(i);
      if (i != 1) {
        expected.append(System.getProperty("line.separator"));
      }
    }

    assertCommandResult(
        runLocal(cmd.toString()),
        expected.toString(),
        "",
        expected.toString(),
        0
    );
  }

  @Test(timeout = 10_000)
  public void given99echoConcatenatedCommands$whenRun$then99LinesWrittenCorrectly() throws Exception {
    LOGGER.info("test-10");
    StringBuilder cmd = new StringBuilder();
    for (int i = 99; i > 0; i--) {
      cmd.append("echo ").append(i);
      if (i != 1) {
        cmd.append(" && ");
      }
    }

    StringBuilder expected = new StringBuilder();
    for (int i = 99; i > 0; i--) {
      expected.append(i);
      if (i != 1) {
        expected.append(System.getProperty("line.separator"));
      }
    }

    assertCommandResult(
        runLocal(cmd.toString()),
        expected.toString(),
        "",
        expected.toString(),
        0
    );
  }

  @Test(timeout = 10_000)
  public void given200echoConcatenatedCommands$whenRun$then100LinesWrittenCorrectly() throws Exception {
    LOGGER.info("test-11");
    StringBuilder cmd = new StringBuilder();
    for (int i = 200; i > 0; i--) {
      cmd.append("echo ").append(i);
      if (i != 1) {
        cmd.append(" && ");
      }
    }

    StringBuilder expected = new StringBuilder();
    for (int i = 100; i > 0; i--) {
      expected.append(i);
      if (i != 1) {
        expected.append(System.getProperty("line.separator"));
      }
    }

    assertCommandResult(
        runLocal(cmd.toString()),
        expected.toString(),
        "",
        expected.toString(),
        0
    );
  }

  @Test(timeout = 10_000)
  public void given201echoConcatenatedCommands$whenRun$then100LinesWrittenCorrectly() throws Exception {
    LOGGER.info("test-12");
    StringBuilder cmd = new StringBuilder();
    for (int i = 201; i > 0; i--) {
      cmd.append("echo ").append(i);
      if (i != 1) {
        cmd.append(" && ");
      }
    }

    StringBuilder expected = new StringBuilder();
    for (int i = 100; i > 0; i--) {
      expected.append(i);
      if (i != 1) {
        expected.append(System.getProperty("line.separator"));
      }
    }

    assertCommandResult(
        runLocal(cmd.toString()),
        expected.toString(),
        "",
        expected.toString(),
        0
    );
  }

  @Test(timeout = 10_000)
  public void givenCommandWrites10KdataEncodedByBase64$whenRunLocally$thenOutisWrittenToStdoutCorrectly() throws Exception {
    LOGGER.info("test-13");
    String cmd = format("cat /dev/zero | head -c 10000 | %s 80", base64());

    String expected = buildExpectedOutputFromDevZeroEncodedByBase64(80, 54);
    // AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
    // 123456789012345678901234567890123456789012345678901234
    assertCommandResult(
        runLocal(cmd),
        expected,
        "",
        expected,
        0
    );
  }

  @Test(timeout = 5000)
  public void givenCommandWrites100KdataEncodedByBase64$whenRunLocally$thenOutisWrittenToStdoutCorrectly() throws Exception {
    LOGGER.info("test-14");
    String cmd = format("cat /dev/zero | head -c 100000 | %s 80", base64());

    String expected = buildExpectedOutputFromDevZeroEncodedByBase64(80, 54);
    // AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
    // 123456789012345678901234567890123456789012345678901234
    assertCommandResult(
        runLocal(cmd),
        expected,
        "",
        expected,
        0
    );
  }

  @Test
  public void givenEchoPipedWithCatN$whenLocally$thenLineNumberedDataWrittenToStdout() throws Exception {
    assertThat(
        runLocal("echo hello | cat -n"),
        allOf(
            matcherBuilder(
                "stdout", CommandResult::stdout
            ).check(
                "containsString'1\thello'", s -> s.contains("1\thello")
            ).build(),
            matcherBuilder(
                "toString", CommandResult::toString
            ).check(
                "equals'...'", s -> s.equals("'echo hello | cat -n' exit with 0:''")
            ).build()
        ));
  }

  /**
   * TODO: Flaky 7/18/2017
   *
   * @throws Exception
   */
  @Test(timeout = 10000)
  public void givenBase64ed1MBRedirectingStdoutToStderr$whenRunLocally$thenDataWrittenToStderr() throws Exception {
    LOGGER.info("test-15");
    String cmd = format("cat /dev/zero | head -c 1000000 | %s 80 >&2", base64());

    String expected = buildExpectedOutputFromDevZeroEncodedByBase64(80, 54);
    // AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
    // 123456789012345678901234567890123456789012345678901234

    assertCommandResult(
        runLocal(cmd),
        "",
        expected,
        expected,
        0
    );
  }

  @Test(timeout = 10000)
  public void givenCommandsWritingLargeDataBothToStderrAndStdout$whenRunLocally$thenDataWrittenBothToStderrAndStdout() throws Exception {
    String cmd = format("cat /dev/zero | head -c 100000 | %s 80 >&2 && cat /dev/zero | head -c 100000 | %s 80", base64(), base64());
    String expected = buildExpectedOutputFromDevZeroEncodedByBase64(80, 54);
    // AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
    // 123456789012345678901234567890123456789012345678901234
    assertCommandResult(
        runLocal(cmd),
        expected,
        expected,
        null,
        0
    );
  }

  /**
   * TODO: Flaky 7/18/2017
   */
  @Test(timeout = 10000)
  public void givenCommandsWritingLargeDataBothToStdoutAndStderr$whenRunLocally$thenDataWrittenBothToStdoutAndStderr() throws Exception {
    String cmd = format("cat /dev/zero | head -c 100000 | %s 80 && cat /dev/zero | head -c 100000 | %s 80 >&2", base64(), base64());

    String expected = buildExpectedOutputFromDevZeroEncodedByBase64(80, 54);
    // AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
    // 123456789012345678901234567890123456789012345678901234

    assertCommandResult(
        runLocal(cmd),
        expected,
        expected,
        null,
        0
    );
  }

  @Test(timeout = 30000)
  public void givenCommandWriting10MdataToStdout$whenRunLocally$thenDataWrittenToStdout() throws Exception {
    LOGGER.info("test-18");
    String cmd = format("cat /dev/zero | head -c 10000000 | %s 80", base64());

    String expected = buildExpectedOutputFromDevZeroEncodedByBase64(80, 54);
    // AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
    // 123456789012345678901234567890123456789012345678901234
    assertCommandResult(
        runLocal(cmd),
        expected,
        "",
        expected,
        0
    );
  }


  /**
   * This test is marked 'ignored' since resource/time consuming.
   */
  //@Ignore
  @Test(timeout = 60000)
  public void givenCommandWriting100MdataToStdout$whenRunLocally$thenDataWrittenToStdout() throws Exception {
    LOGGER.info("test-18");
    String cmd = format("cat /dev/zero | head -c 100000000 | %s 80", base64());

    String expected = buildExpectedOutputFromDevZeroEncodedByBase64(80, 54);
    // AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
    // 123456789012345678901234567890123456789012345678901234
    assertCommandResult(
        runLocal(cmd),
        expected,
        "",
        expected,
        0
    );
  }

  /**
   * This test requires your account is able to login localhost over SSH without
   * any password intervention.
   * TODO: Flaky 7/18/2017
   */
  @Test(timeout = 20_000)
  public void givenEchoCommand$whenRunWithSsh$thenExepectedOutputIsWrittenBothToStdoutAndStderr() throws Exception {
    LOGGER.info("test-19");
    boolean finished = false;
    try {
      String userName = TestUtils.userName();
      String hostName = TestUtils.hostName();
      String privKey = TestUtils.identity();

      assertThat(
          runRemote(userName, hostName, privKey, "echo hello1"),
          allOf(
              matcherBuilder("stdout", CommandResult::stdout).check("=='hello1'", s -> s.equals("hello1")).build(),
              anyOf(
                  matcherBuilder("stdout", CommandResult::stderr).check("==''", s -> s.equals("")).build(),
                  matcherBuilder("stdout", CommandResult::stderr).check("startsWith'Warning: Permanently added the RSA host key for IP address'", s -> s.startsWith("Warning: Permanently added the RSA host key for IP address")).build()
              ),
              matcherBuilder("exitCode", CommandResult::exitCode).check("==0", e -> e == 0).build()
          ));
      finished = true;
    } finally {
      if (!finished) {
        LOGGER.error("In order to make this test pass, you have to configure your passphraseless ssh key");
      }
    }
  }

  /**
   * TODO: Flaky 7/18/2017
   */
  @Test(timeout = 10_000)
  public void givenEchoCommand$whenRunRemotely$thenOutputIsWrittenToStdoutCorrectly() throws Exception {
    LOGGER.info("test-20");
    String userName = runLocal("whoami").stdout();
    String hostName = runLocal("hostname").stdout();

    assertCommandResult(
        runRemote(userName, hostName, TestUtils.identity(), "echo hello"),
        "hello",
        "",
        "hello",
        0
    );

  }

  @Test(expected = CommandTimeoutException.class, timeout = 10_000)
  public void givenCommandThatTakes3secs$whenRunLocallyWith1secTimeout$thenCommandTimeoutExceptionThrown() throws Exception {
    LOGGER.info("test-21");
    try {
      runLocal(1000, "sleep 3").exitCode();
      TestCase.fail("The command didn't time out in 1 sec!");
    } finally {
      ////
      // Sleep 2.5sec to make sure the spawned process goes away
      Thread.sleep(2500);
    }
  }

  @Test(expected = CommandTimeoutException.class, timeout = 10_000)
  public void givenCommandThatTakes3secs$whenRunRemotelyWith1secTimeout$thenCommandTimeoutExceptionThrown() throws Exception {
    LOGGER.info("test-22");
    try {
      String userName = runLocal("whoami").stdout();
      String hostName = runLocal("hostname").stdout();

      runRemote(1000, userName, hostName, TestUtils.identity(), "sleep 3");
      TestCase.fail("The command didn't time out in 1 sec!");
    } finally {
      ////
      // Sleep 2.5sec to make sure the spawned process goes away
      Thread.sleep(100);
    }
  }

  @Test(timeout = 20_000)
  public void givenEchoHello$whenRunRemotely$thenExpectedOutputIsWrittenToStdout() throws Exception {
    LOGGER.info("test-23");
    String userName = runLocal("whoami").stdout();
    String hostName = runLocal("hostname").stdout();

    assertCommandResult(
        runRemote(15_000, userName, hostName, TestUtils.identity(), "echo hello"),
        "hello",
        "",
        "hello",
        0
    );
  }

  private static void assertCommandResult(CommandResult result, String stdout, String stderr, String stdouterr, int exitCode) {
    assertThat(
        result,
        allOf(
            matcherBuilder(
                "stdout", CommandResult::stdout
            ).check(
                "=='" + stdout + "'", s -> s.equals(stdout)
            ).build(),
            matcherBuilder(
                "stderr", CommandResult::stderr
            ).check(
                "=='" + stderr + "'", s -> s.equals(stderr)
            ).build(),
            stdouterr != null ?
                matcherBuilder(
                    "stdouterr", CommandResult::stdouterr
                ).check(
                    "=='" + stdouterr + "'", s -> s.equals(stdouterr)
                ).build() :
                CoreMatchers.anything()
            ,
            matcherBuilder(
                "exitCode", CommandResult::exitCode
            ).check(
                "==" + exitCode, s -> s.equals(exitCode)
            ).build()
        ));
  }

  private static String buildExpectedOutputFromDevZeroEncodedByBase64(@SuppressWarnings("SameParameterValue") int numCharsPerOneLine, @SuppressWarnings("SameParameterValue") int numCharsInLastLine) {
    StringBuilder b = new StringBuilder();
    for (int i = 1; i < 100; i++) {
      b.append(buildExpectedDataOneLine(numCharsPerOneLine));
      b.append(System.getProperty("line.separator"));
    }
    b.append(buildExpectedDataOneLine(numCharsInLastLine));
    b.append("==");
    return b.toString();
  }

  private static String buildExpectedDataOneLine(int numChars) {
    StringBuilder b = new StringBuilder(numChars * 2);
    for (int i = 0; i < numChars; i++) {
      b.append("A");
    }
    return b.toString();
  }
}

