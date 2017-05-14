package com.github.dakusui.cmd;

public enum TestUtils {
  ;
  public static String identity() {
    String key = "commandstreamer.identity";
    if (!System.getProperties().containsKey(key))
     return String.format("%s/.ssh/id_rsa", CommandUtils.runLocal("echo $HOME").stdout());
    return System.getProperty(key);
  }
}
