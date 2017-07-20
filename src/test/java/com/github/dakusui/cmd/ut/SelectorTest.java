package com.github.dakusui.cmd.ut;

import com.github.dakusui.cmd.StreamableQueue;
import com.github.dakusui.cmd.core.IoUtils;
import com.github.dakusui.cmd.core.Selector;
import com.github.dakusui.cmd.utils.TestUtils;
import org.junit.Test;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.github.dakusui.cmd.core.Selector.select;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;

public class SelectorTest extends TestUtils.TestBase {
  /**
   * TODO: Flakiness was seen (7/18/2017)
   * <pre>
   *   B-99
   * shutting to
   *
   * given3Streams$whenSelect$thenAllElementsFoundInOutputAndInterleaved(com.github.dakusui.cmd.ut.SelectorTest)  Time elapsed: 0.142 sec  <<< ERROR!
   * java.lang.NullPointerException
   * at java.util.LinkedList$ListItr.next(LinkedList.java:893)
   * at java.lang.Iterable.forEach(Iterable.java:74)
   * at com.github.dakusui.cmd.ut.SelectorTest.given3Streams$whenSelect$thenAllElementsFoundInOutputAndInterleaved(SelectorTest.java:60)
   * at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
   * at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
   * at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
   * at java.lang.reflect.Method.invoke(Method.java:497)
   * at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:50)
   * at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
   * at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:47)
   * at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
   * at org.junit.internal.runners.statements.FailOnTimeout$CallableStatement.call(FailOnTimeout.java:298)
   * at org.junit.internal.runners.statements.FailOnTimeout$CallableStatement.call(FailOnTimeout.java:292)
   * at java.util.concurrent.FutureTask.run(FutureTask.java:266)
   * at java.lang.Thread.run(Thread.java:745)
   *
   *
   * </pre>
   */
  @Test(timeout = 5_000)
  public void given3Streams$whenSelect$thenAllElementsFoundInOutputAndInterleaved() {
    List<String> out = Collections.synchronizedList(new LinkedList<>());

    try {
      Selector<String> selector = createSelector(50, 100, 200);
      selector.stream().forEach(
          ((Consumer<String>) s -> {
            System.err.println("taken:" + s);
          }).andThen(
              out::add
          )
      );
      out.forEach(System.out::println);
      //noinspection unchecked
      assertThat(
          out,
          TestUtils.allOf(
              TestUtils.<List<String>, Integer>matcherBuilder()
                  .transform("sizeOf", List::size)
                  .check("50+100==", u -> 50 + 100 == u)
                  .build(),
              TestUtils.MatcherBuilder.<List<String>>simple()
                  .check("interleaving", u -> !u.equals(u.stream().sorted().collect(toList())))
                  .build()

          ));
    } finally {
      System.out.println("shutting to");
    }
  }

  @Test(timeout = 5_000)
  public void select100Kdata() {
    select(
        TestUtils.<String>list("A", 100_000).stream(),
        TestUtils.<String>list("B", 100_000).stream(),
        TestUtils.<String>list("C", 100_000).stream()
    ).forEach(System.out::println);
  }

  @Test(timeout = 5_000)
  public void select100KdataUneven() {
    select(
        TestUtils.<String>list("A", 100).stream(),
        TestUtils.<String>list("B", 100_000).stream(),
        TestUtils.<String>list("C", 100_000).stream()
    ).forEach(System.out::println);
  }

  @Test
  public void select100Kdata2() {
    StreamableQueue<String> down = new StreamableQueue<>(100);
    Stream<String> up = Stream.concat(TestUtils.list("stdin", 100_000).stream(), Stream.of((String) null)).peek(down);
    new Thread(() -> {
      up.forEach(IoUtils.nop());
    }).start();
    new Selector.Builder<String>(
        "UT"
    ).add(
        TestUtils.<String>list("stdout", 100_000).stream(),
        IoUtils.nop(),
        true
    ).add(
        TestUtils.<String>list("stderr", 100_000).stream(),
        System.err::println,
        false
    ).build(
    ).stream().forEach(
        System.out::println
    );
  }

  private Selector<String> createSelector(int sizeA, int sizeB, int sizeC) {
    return new Selector.Builder<String>(
        "UT"
    ).add(
        TestUtils.list("A", sizeA).stream().filter(s -> sleepAndReturn(true)),
        IoUtils.nop(),
        true
    ).add(
        TestUtils.list("B", sizeB).stream().filter(s -> sleepAndReturn(true)),
        IoUtils.nop(),
        true
    ).add(
        TestUtils.list("C", sizeC).stream().filter(s -> sleepAndReturn(false)),
        IoUtils.nop(),
        true
    ).build();
  }

  private static boolean sleepAndReturn(boolean value) {
    if (value) {
      System.out.println("sleepAndReturn:start:" + Thread.currentThread().getId());
      if (Thread.currentThread().isInterrupted()) {
        System.out.println("sleepAndReturn:interrupted(1):" + Thread.currentThread().getId());
        return true;
      }
      try {
        Thread.sleep(1);
      } catch (InterruptedException ignored) {
        System.out.println("sleepAndReturn:interrupted(2):" + Thread.currentThread().getId());
      }
      System.out.println("sleepAndReturn:end:" + Thread.currentThread().getId());
    }
    return value;
  }
}
