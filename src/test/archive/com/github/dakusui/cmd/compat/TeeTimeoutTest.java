package com.github.dakusui.cmd.compat;

import com.github.dakusui.cmd.compat.Tee;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import static com.github.dakusui.cmd.exceptions.Exceptions.wrap;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TeeTimeoutTest {
  @Test(timeout = 5_000)
  public void timesOut() throws InterruptedException {
    List<Integer> out = new LinkedList<>();
    assertFalse(
        Tee.tee(
            Stream.of(1, 2, 3)
        ).timeOut(
            1, MILLISECONDS
        ).connect(
            integer -> {
              try {
                out.add(integer);
                Thread.sleep(1000);
              } catch (InterruptedException ignored) {
                throw wrap(ignored);
              }
            }
        ).run()
    );

    assertTrue(out.size() <= 1);
  }

  @Test(timeout = 5_000)
  public void timesOut2() throws InterruptedException {
    List<Integer> out = new LinkedList<>();
    assertFalse(
        new Tee.Connector<>(
            Stream.of(1, 2, 3, 4, 5)
        ).timeOut(
            30, MILLISECONDS
        ).connect(
            integer -> {
              try {
                out.add(integer);
                Thread.sleep(10);
              } catch (InterruptedException ignored) {
                throw wrap(ignored);
              }
            }
        ).run()
    );

    assertTrue(out.size() < 5);
    assertTrue(out.size() > 1);
  }

}
