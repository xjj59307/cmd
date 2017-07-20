package com.github.dakusui.cmd.ut;

import com.github.dakusui.cmd.Cmd;
import com.github.dakusui.cmd.utils.TestUtils;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static com.github.dakusui.cmd.Cmd.cat;
import static com.github.dakusui.cmd.Cmd.cmd;
import static org.junit.Assert.assertEquals;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PipelinedCmdTest extends TestUtils.TestBase {
  @Test
  public void simplePipe() {
    AtomicInteger c = new AtomicInteger(0);

    cmd(
        "echo hello && echo world"
    ).connect(
        cmd(
            "cat -n"
        ).connect(
            cmd("cat -n")
        )
    ).stream().map(
        s -> String.format("<%s>", s)
    ).peek(
        s -> c.getAndIncrement()
    ).forEach(
        System.out::println
    );

    assertEquals(2, c.get());
  }

  /**
   * Shows flakiness 7/18/2017
   * <code>
   * org.junit.runners.model.TestTimedOutException: test timed out after 5000 milliseconds
   * <p>
   * at java.io.FileInputStream.readBytes(Native Method)
   * at java.io.FileInputStream.read(FileInputStream.java:255)
   * at java.io.BufferedInputStream.read1(BufferedInputStream.java:284)
   * at java.io.BufferedInputStream.read(BufferedInputStream.java:345)
   * at java.io.BufferedInputStream.read1(BufferedInputStream.java:284)
   * at java.io.BufferedInputStream.read(BufferedInputStream.java:345)
   * at sun.nio.cs.StreamDecoder.readBytes(StreamDecoder.java:284)
   * at sun.nio.cs.StreamDecoder.implRead(StreamDecoder.java:326)
   * at sun.nio.cs.StreamDecoder.read(StreamDecoder.java:178)
   * at java.io.InputStreamReader.read(InputStreamReader.java:184)
   * at java.io.BufferedReader.fill(BufferedReader.java:161)
   * at java.io.BufferedReader.readLine(BufferedReader.java:324)
   * at java.io.BufferedReader.readLine(BufferedReader.java:389)
   * at com.github.dakusui.cmd.core.IoUtils$3.readLine(IoUtils.java:133)
   * at com.github.dakusui.cmd.core.IoUtils$3.readIfNotReadYet(IoUtils.java:124)
   * at com.github.dakusui.cmd.core.IoUtils$3.hasNext(IoUtils.java:106)
   * at java.util.Iterator.forEachRemaining(Iterator.java:115)
   * at java.util.Spliterators$IteratorSpliterator.forEachRemaining(Spliterators.java:1801)
   * at java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:481)
   * at java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:471)
   * </code>
   */
  @Test(timeout = 5_000)
  public void complexPipe() {
    AtomicInteger c = new AtomicInteger(0);

    cmd("echo hello && echo world").connect(
        cmd("cat -n").connect(
            cmd("sort -r").connect(
                cmd("sed 's/hello/HELLO/'").connect(
                    cmd("sed -E 's/^ +//'")
                )))
    ).stream(
    ).map(
        s -> String.format("<%s>", s)
    ).peek(
        s -> c.getAndIncrement()
    ).forEach(
        System.out::println
    );

    assertEquals(2, c.get());
  }

  @Test(timeout = 5_000)
  public void pipedCommands() {
    AtomicInteger c = new AtomicInteger(0);

    cmd("echo hello && echo world").connect(
        cmd("cat -n | sort -r | sed 's/hello/HELLO/' | sed -E 's/^ +//'")
    ).stream(
    ).map(
        s -> String.format("<%s>", s)
    ).peek(
        s -> c.getAndIncrement()
    ).forEach(
        System.out::println
    );

    assertEquals(2, c.get());
  }

  @Test(timeout = 15_000)
  public void tee10K() {
    AtomicInteger c = new AtomicInteger(0);

    Cmd.cmd(
        "seq 1 10000"
    ).readFrom(
        () -> Stream.of((String) null)
    ).connect(
        cat().pipeline(
            stream -> stream.map(
                s -> "LEFT:" + s
            )
        )
    ).connect(
        cat().pipeline(
            stream -> stream.map(
                s -> "RIGHT:" + s
            )
        )
    ).stream(
    ).peek(
        s -> c.getAndIncrement()
    ).forEach(
        System.out::println
    );

    assertEquals(20_000, c.get());
  }

  @Test(timeout = 30_000)
  public void tee20K() {
    AtomicInteger c = new AtomicInteger(0);

    cmd(
        "seq 1 20000"
    ).connect(
        cat().pipeline(
            stream -> stream.map(
                s -> "LEFT:" + s
            )
        )
    ).connect(
        cat().pipeline(
            stream -> stream.map(
                s -> "RIGHT:" + s
            )
        )
    ).stream(
    ).peek(
        s -> c.getAndIncrement()
    ).forEach(
        System.out::println
    );

    assertEquals(40_000, c.get());
  }

  @Test(timeout = 15_000)
  public void pipe10K() throws InterruptedException {
    AtomicInteger c = new AtomicInteger(0);

    cmd(
        "seq 1 10000"
    ).connect(
        cat().pipeline(
            st -> st.map(
                s -> "DOWN:" + s
            )
        )
    ).stream(
    ).peek(
        s -> c.getAndIncrement()
    ).forEach(
        System.out::println
    );

    assertEquals(10000, c.get());
  }

  @Test(timeout = 15_000)
  public void pipe20K() throws InterruptedException {
    AtomicInteger c = new AtomicInteger(0);

    cmd(
        "seq 1 20000"
    ).connect(
        cat().pipeline(
            st -> st.map(
                s -> "DOWN:" + s
            )
        )
    ).stream(
    ).peek(
        s -> c.getAndIncrement()
    ).forEach(
        System.err::println
    );

    assertEquals(20_000, c.get());
  }

  @Test(timeout = 30_000)
  public void pipe100K() throws InterruptedException {
    AtomicInteger c = new AtomicInteger(0);

    cmd(
        "seq 1 100000"
    ).connect(
        cat().pipeline(
            st -> st.map(
                s -> "DOWN:" + s
            )
        )
    ).stream(
    ).peek(
        s -> c.getAndIncrement()
    ).forEach(
        System.out::println
    );

    assertEquals(100000, c.get());
  }
}
