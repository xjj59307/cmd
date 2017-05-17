package com.github.dakusui.cmd;

import org.junit.After;
import org.junit.Before;

import java.io.*;

public enum TestUtils {
  ;

  static final PrintStream STDOUT = System.out;
  static final PrintStream STDERR = System.err;

  /**
   * A base class for tests which writes to stdout/stderr.
   */
  public static class StdOutTestBase {
    @Before
    public void suppressStdOutErrIfRunUnderSurefire() {
      TestUtils.suppressStdOutErrIfRunUnderSurefire();
    }

    @After
    public void restoreStdOutErr() {
      TestUtils.restoreStdOutErr();
    }
  }


  public static void suppressStdOutErrIfRunUnderSurefire() {
    if (TestUtils.isRunUnderSurefire()) {
      System.setOut(new PrintStream(new OutputStream() {
        @Override
        public void write(int b) throws IOException {
        }
      }));
      System.setErr(new PrintStream(new OutputStream() {
        @Override
        public void write(int b) throws IOException {
        }
      }));
    }
  }

  public static void restoreStdOutErr() {
    System.setOut(STDOUT);
    System.setOut(STDERR);
  }

  public static boolean isRunUnderSurefire() {
    return System.getProperty("surefire.real.class.path") != null;
  }

  public static String identity() {
    String key = "commandstreamer.identity";
    if (!System.getProperties().containsKey(key))
      return String.format("%s/.ssh/id_rsa", CommandUtils.runLocal("echo $HOME").stdout());
    return System.getProperty(key);
  }

  public static String userName() {
    String key = "commandstreamer.username";
    if (!System.getProperties().contains(key))
      return System.getProperty("user.name");
    return System.getProperty(key);
  }

  public static InputStream openForRead(File file) {
    try {
      return new FileInputStream(file);
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public static OutputStream openForWrite(File file) {
    try {
      return new FileOutputStream(file);
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  private static File createTempFile() {
    try {
      File ret = File.createTempFile("commandrunner-streamable-", ".log");
      ret.deleteOnExit();
      return ret;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
