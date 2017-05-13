package com.github.dakusui.cmd;

public enum TestUtils {
  ;
  static {
    System.setProperty("commandstreamer.identity", "/Users/hiroshi.ukai/.ssh/id_rsa.p25283");
  }

  public static String identity() {
    String key = "commandstreamer.identity";
    if (!System.getProperties().containsKey(key))
     return String.format("%s/.ssh/id_rsa", CommandUtils.runLocal("echo $HOME").stdout());
    return System.getProperty(key);
  }
}
