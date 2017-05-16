package com.github.dakusui.cmd;

import java.io.*;

public enum TestUtils {
  ;
  public static String identity() {
    String key = "commandstreamer.identity";
    if (!System.getProperties().containsKey(key))
     return String.format("%s/.ssh/id_rsa", CommandUtils.runLocal("echo $HOME").stdout());
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
