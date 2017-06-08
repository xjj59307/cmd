package com.github.dakusui.cmd.ut;

import com.github.dakusui.cmd.core.Tee;
import com.github.dakusui.cmd.utils.TestUtils;
import com.github.dakusui.jcunit8.factorspace.Parameter;
import com.github.dakusui.jcunit8.runners.junit4.JCUnit8;
import com.github.dakusui.jcunit8.runners.junit4.annotations.Condition;
import com.github.dakusui.jcunit8.runners.junit4.annotations.From;
import com.github.dakusui.jcunit8.runners.junit4.annotations.Given;
import com.github.dakusui.jcunit8.runners.junit4.annotations.ParameterSource;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertThat;

@RunWith(JCUnit8.class)
public class TeeTest {
  @ParameterSource
  public Parameter.Factory<List<String>> data() {
    return Parameter.Simple.Factory.of(asList(
        asList("1", "2", "3"),
        asList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j"),
        createList(1_000),
        createList(10_000),
        emptyList()
    ));
  }

  @ParameterSource
  public Parameter.Factory<Boolean> withIntervals() {
    return Parameter.Simple.Factory.of(asList(
        true,
        false
    ));
  }

  @ParameterSource
  public Parameter.Factory<Integer> numStreams() {
    return Parameter.Simple.Factory.of(asList(
        1, 2, 3, 4, 5, 10, 32
    ));
  }

  @ParameterSource
  public Parameter.Factory<Integer> queueSize() {
    return Parameter.Simple.Factory.of(asList(
        1, 2, 100, 16384
    ));
  }

  @Test
  public void printTestCase(
      @From("data") List<String> data,
      @From("numStreams") int numStreams,
      @From("queueSize") int queueSize
  ) {
    System.err.printf("numStreams=%s,queueSize=%s, data=%s%n", numStreams, queueSize, data);
  }

  @Test(timeout = 1_000)
  @Given("isDataEmpty")
  public void givenEmptyData$whenDoTee$thenNothingIsInOutput(
      @From("data") List<String> data,
      @From("numStreams") int numStreams,
      @From("queueSize") int queueSize
  ) throws InterruptedException {
    assertThat(
        runTeeWithSpecifiedNumberOfDownStreams(false, data, numStreams, queueSize),
        TestUtils.<List<String>, Integer>matcherBuilder()
            .transform("size", List::size)
            .check("==0", size -> size == 0).build()
    );
  }

  @Test(timeout = 2_000)
  @Given("!isDataEmpty&&smallData")
  public void givenNonEmptyData$whenDoTeeWithIntervals$thenRunsNormally(
      @From("data") List<String> data,
      @From("withIntervals") boolean withIntervals,
      @From("numStreams") int numStreams,
      @From("queueSize") int queueSize
  ) throws InterruptedException {
    assertThat(
        runTeeWithSpecifiedNumberOfDownStreams(withIntervals, data, numStreams, queueSize),
        TestUtils.<List<String>, Integer>matcherBuilder()
            .transform("size", List::size)
            .check("==numStreams*data.size()", v -> v == numStreams * data.size()).build()
    );
  }

  @Test(timeout = 8_000)
  @Given("!isDataEmpty&&isQueueSizeBig")
  public void givenNonEmptyData$whenDoTeeWithBigQueueSize$thenRunsNormally(
      @From("data") List<String> data,
      @From("numStreams") int numStreams,
      @From("queueSize") int queueSize
  ) throws InterruptedException {
    assertThat(
        runTeeWithSpecifiedNumberOfDownStreams(false, data, numStreams, queueSize),
        TestUtils.<List<String>, Integer>matcherBuilder()
            .transform("size", List::size)
            .check("==numStreams*data.size()", v -> v == numStreams * data.size()).build()
    );
  }

  @Test(timeout = 15_000)
  @Given("!isDataEmpty&&!isQueueSizeBig")
  public void givenNonEmptyData$whenDoTee$thenRunsNormally(
      @From("data") List<String> data,
      @From("numStreams") int numStreams,
      @From("queueSize") int queueSize
  ) throws InterruptedException {
    assertThat(
        runTeeWithSpecifiedNumberOfDownStreams(false, data, numStreams, queueSize),
        TestUtils.<List<String>, Integer>matcherBuilder()
            .transform("size", List::size)
            .check(format("==%d*%d", numStreams, data.size()), v -> v == numStreams * data.size()).build()
    );
  }

  @Condition
  public boolean isDataEmpty(@From("data") List<String> data) {
    return data.isEmpty();
  }

  @Condition
  public boolean smallData(@From("data") List<String> data) {
    return data.size() < 100;
  }

  @Condition
  public boolean isQueueSizeBig(@From("queueSize") int queueSize) {
    return queueSize > 5;
  }

  private List<String> runTeeWithSpecifiedNumberOfDownStreams(
      boolean withIntervals,
      List<String> data,
      int numStreams,
      int queueSize
  ) throws InterruptedException {
    List<String> output = Collections.synchronizedList(new LinkedList<>());
    List<Consumer<String>> downstreams = createDownstreamConsumers(withIntervals, numStreams, output);
    Tee.Connector<String> connector = Tee.tee(
        data.stream(), queueSize
    );
    downstreams.forEach(connector::connect);
    connector.run();
    return output;
  }

  private List<Consumer<String>> createDownstreamConsumers(boolean withIntervals, int numStreams, List<String> output) {
    List<Consumer<String>> downstreams = new LinkedList<>();
    for (int i = 0; i < numStreams; i++) {
      downstreams.add(
          (String s) -> {

          }
      );
      downstreams.add(
          ((Consumer<String>) s -> {
            if (withIntervals)
              if (System.nanoTime() % 2 == 0)
                try {
                  Thread.sleep(1);
                } catch (InterruptedException ignored) {
                }
          }).andThen(s ->
              output.add(format("%02d", Thread.currentThread().getId()) + ":" + s)
          )
      );
    }
    return downstreams;
  }

  private List<String> createList(int size) {
    List<String> ret = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      ret.add(String.format("Hello-%04d", i));
    }
    return ret;
  }
}
