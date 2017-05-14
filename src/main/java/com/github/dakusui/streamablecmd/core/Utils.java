package com.github.dakusui.streamablecmd.core;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;

public enum Utils {
  ;

  public static InputStream openForRead(File file) {
    try {
      return new FileInputStream(file);
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public static Stream<String> toStream(InputStream is, Charset charset) {
    return StreamSupport.stream(
        ((Iterable<String>) () -> toIterator(is, charset)).spliterator(),
        false
    );
  }

  private enum IteratorState {
    READ,
    NOT_READ,
    END
  }

  public static Iterator<String> toIterator(InputStream is, Charset charset) {
    return new Iterator<String>() {
      BufferedReader reader = new BufferedReader(
          new InputStreamReader(
              is,
              charset
          )
      );
      IteratorState state = IteratorState.NOT_READ;
      String next;

      @Override
      public boolean hasNext() {
        readIfNotReadYet();
        return state != IteratorState.END;
      }

      @Override
      public String next() {
        if (state == IteratorState.END)
          throw new NoSuchElementException();
        readIfNotReadYet();
        try {
          return next;
        } finally {
          state = IteratorState.NOT_READ;
        }
      }

      private void readIfNotReadYet() {
        if (state == IteratorState.NOT_READ) {
          this.next = readLine(reader);
          state = this.next == null ?
              IteratorState.END :
              IteratorState.READ;
        }
      }

      private String readLine(BufferedReader reader) {
        try {
          return reader.readLine();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    };
  }

  public static OutputStream openForWrite(File file) {
    try {
      return new FileOutputStream(file);
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * If {@code null} is given to a consumer returned by this method, the output
   * stream {@code os} will be closed and {@code null} will not be passed to it.
   *
   * @param os      OutputStream to which string objects given to returned consumer written.
   * @param charset A {@code Charset} object that specifies encoding by which
   */
  public static Consumer<String> toConsumer(OutputStream os, Charset charset) {
    try {
      PrintStream ps = new PrintStream(os, true, charset.displayName());
      return s -> {
        if (s != null)
          ps.println(s);
        else
          ps.close();
      };
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  public static class Drainer<T> {

    private final ExecutorService executorService;
    private final List<Runnable>  runnables;

    public Drainer(Map<Stream<T>, Consumer<T>> streams, ExecutorService executorService) {
      this.executorService = executorService;
      this.runnables = streams.entrySet().stream()
          .map(each -> toRunnable(each.getKey(), each.getValue()))
          .collect(toList());
    }

    private Runnable toRunnable(Stream<T> stream, Consumer<T> consumer) {
      return () -> stream.forEach(consumer);
    }

    public void drain() {
      runnables.forEach(executorService::submit);
    }

    public static class Builder<T> {
      private Map<Stream<T>, Consumer<T>> streams = new HashMap<>();

      public Builder<T> add(Stream<T> stream, Consumer<T> consumer) {
        streams.put(stream, consumer);
        return this;
      }

      public Drainer<T> build() {
        return new Drainer<T>(this.streams, Executors.newFixedThreadPool(2));
      }
    }
  }

  public static void main(String... args) throws InterruptedException {
    Queue<String> queue = new ArrayBlockingQueue<>(200);
    new Drainer.Builder<String>()
        .add(
            list("A", 100).stream(),
            queue::offer
        )
        .add(
            list("B", 100).stream(),
            queue::offer
        )
        .add(
            list("C", 100).stream(),
            queue::offer
        )
        .build()
        .drain();
    Thread.sleep(100);

    while (true) {
      String s = queue.poll();
      if (s != null)
        System.out.println(s);
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
