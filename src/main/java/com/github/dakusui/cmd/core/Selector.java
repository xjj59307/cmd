package com.github.dakusui.cmd.core;

import com.github.dakusui.cmd.exceptions.Exceptions;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;

public class Selector<T> {
  private static final Object SENTINEL  = new Object() {
    @Override
    public String toString() {
      return "SENTINEL";
    }
  };
  private static final Object READ_NEXT = new Object() {
    @Override
    public String toString() {
      return "READ_NEXT";
    }
  };

  private final Map<Stream<T>, Consumer<Object>> streams;
  private final ExecutorService                  executorService;
  private final BlockingQueue<Object>            queue;

  Selector(Map<Stream<T>, Consumer<Object>> streams, BlockingQueue<Object> queue, ExecutorService executorService) {
    this.streams = new LinkedHashMap<Stream<T>, Consumer<Object>>() {{
      putAll(streams);
    }};
    this.executorService = executorService;
    this.queue = queue;
  }


  public Stream<T> select() {
    drain(
        this.streams,
        this.executorService
    );
    return StreamSupport.stream(
        ((Iterable<T>) () -> new Iterator<T>() {
          Object next = READ_NEXT;

          @Override
          public boolean hasNext() {
            if (next == READ_NEXT)
              next = takeFrom(queue);
            return next != SENTINEL;
          }

          private Object takeFrom(BlockingQueue<Object> queue) {
            try {
              return queue.take();
            } catch (InterruptedException e) {
              throw Exceptions.wrap(e);
            }
          }

          @SuppressWarnings("unchecked")
          @Override
          public T next() {
            if (next == SENTINEL)
              throw new NoSuchElementException();
            try {
              return (T) next;
            } finally {
              next = READ_NEXT;
            }
          }
        }).spliterator(),
        false
    );
  }

  private static <T> void drain(Map<Stream<T>, Consumer<Object>> streams, ExecutorService executorService) {
    streams.entrySet().stream()
        .map(
            (Function<Map.Entry<Stream<T>, Consumer<Object>>, Runnable>)
                (Map.Entry<Stream<T>, Consumer<Object>> entry) -> () -> appendSentinel(entry.getKey()).forEach(entry.getValue()))
        .map(executorService::submit)
        .collect(toList());
  }

  private static <T> Stream<Object> appendSentinel(Stream<T> stream) {
    return Stream.concat(stream, Stream.of(SENTINEL));
  }

  public static class Builder<T> {
    private final Map<Stream<T>, Consumer<T>> streams         = new LinkedHashMap<>();
    private       ExecutorService             executorService = null;
    private       BlockingQueue<Object>       queue           = new ArrayBlockingQueue<>(100);

    public Builder() {
    }

    public Builder<T> setQueueSize(int sizeOfQueue) {
      this.queue = new ArrayBlockingQueue<>(sizeOfQueue);
      return this;
    }

    public Builder<T> add(Stream<T> stream) {
      return add(stream, null);
    }

    public Builder<T> add(Stream<T> stream, Consumer<T> consumer) {
      this.streams.put(stream, consumer);
      return this;
    }

    public Builder<T> withExecutorService(ExecutorService executorService) {
      this.executorService = executorService;
      return this;
    }

    public Selector<T> build() {

      Consumer<Object> defaultConsumer = new Consumer<Object>() {
        int numConsumedSentinels = 0;
        final int numStreamsForThisConsumer = (int) streams.values().stream().filter(Objects::isNull).count();

        @Override
        public synchronized void accept(Object t) {
          if (t != SENTINEL || (++numConsumedSentinels == numStreamsForThisConsumer))
            putInQueue(t);
        }

        private void putInQueue(Object t) {
          try {
            Builder.this.queue.put(t);
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
        }
      };
      return new Selector<>(
          new LinkedHashMap<Stream<T>, Consumer<Object>>() {{
            Builder.this.streams.forEach(
                (stream, consumer) -> put(
                    stream,
                    consumer != null ?
                        (Consumer<Object>) o -> {
                          if (o != SENTINEL)
                            //noinspection unchecked
                            consumer.accept((T) o);
                        } :
                        defaultConsumer
                )
            );
          }},
          this.queue,
          Objects.requireNonNull(this.executorService)
      );
    }
  }
}
