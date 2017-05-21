package com.github.dakusui.cmd.ut;

import com.github.dakusui.cmd.core.Selector;
import com.github.dakusui.cmd.utils.TestUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;

public class SelectorTest extends TestUtils.TestBase {
  @Test(timeout = 5_000)
  public void main() {
    List<String> out = new LinkedList<>();
    ExecutorService executorService = Executors.newFixedThreadPool(3);
    try {
      Selector<String> selector = new Selector.Builder<String>(3)
          .add(list("A", 5).stream().filter(s -> TestUtils.sleepAndReturn(true)))
          .add(list("B", 10).stream().filter(s -> TestUtils.sleepAndReturn(true)))
          .add(list("C", 20).stream().filter(s -> TestUtils.sleepAndReturn(false)))
          .withExecutorService(executorService)
          .build();
      try {
        selector.select().forEach(s -> {
          System.err.println("taken:" + s);
          out.add(s);
        });
      } finally {
        selector.close();
      }
      out.forEach(System.out::println);
      //noinspection unchecked
      assertThat(out, CoreMatchers.allOf(
          TestUtils.MatcherBuilder.<List<String>, Integer>matcherBuilder()
              .f("sizeOf", List::size)
              .p("5+10==", u -> 5 + 10 == u)
              .build(),
          TestUtils.MatcherBuilder.<List<String>>simple()
              .p("interleaving", u -> !u.equals(u.stream().sorted().collect(toList())))
              .build()

      ));
    } finally {
      System.out.println("shutting down");
      executorService.shutdown();
    }
  }

  private static List<String> list(String prefix, int size) {
    List<String> ret = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      ret.add(String.format("%s-%s", prefix, i));
    }
    return ret;
  }
}
