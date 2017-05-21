package com.github.dakusui.cmd.ut.io;

import com.github.dakusui.cmd.io.RingBufferedLineWriter;
import com.github.dakusui.cmd.utils.TestUtils;
import org.junit.Assert;
import org.junit.Test;

public class RingBufferedLineWriterTest extends TestUtils.TestBase {
  @Test
  public void write_hello_once() {
    RingBufferedLineWriter ringWriter = new RingBufferedLineWriter(3);
    ringWriter.write("hello");

    Assert.assertEquals("hello", ringWriter.asString());
  }

  @Test
  public void write_hello_twice() {
    RingBufferedLineWriter ringWriter = new RingBufferedLineWriter(3);
    ringWriter.write("hello1");
    ringWriter.write("hello2");

    Assert.assertEquals("hello1\nhello2", ringWriter.asString());
  }

  @Test
  public void write_hello_3times() {
    RingBufferedLineWriter ringWriter = new RingBufferedLineWriter(3);
    ringWriter.write("hello1");
    ringWriter.write("hello2");
    ringWriter.write("hello3");

    Assert.assertEquals("hello1\nhello2\nhello3", ringWriter.asString());
  }

  @Test
  public void write_hello_4times() {
    RingBufferedLineWriter ringWriter = new RingBufferedLineWriter(3);
    ringWriter.write("hello1");
    ringWriter.write("hello2");
    ringWriter.write("hello3");
    ringWriter.write("hello4");

    Assert.assertEquals("hello2\nhello3\nhello4", ringWriter.asString());
  }

  @Test
  public void write_none() {
    RingBufferedLineWriter ringWriter = new RingBufferedLineWriter(3);

    Assert.assertEquals("", ringWriter.asString());
  }


}
