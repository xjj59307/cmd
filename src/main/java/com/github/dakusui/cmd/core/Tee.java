package com.github.dakusui.cmd.core;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;

public class Tee<T> extends Thread {
  private static final Object SENTINEL = new Object() {
    @Override
    public String toString() {
      return "SENTINEL";
    }
  };
  private final Stream<T>           in;
  private final List<Queue<Object>> queues;
  private final List<Stream<T>>     streams;

  Tee(Stream<T> in, int numDownStreams, int queueSize) {
    this.in = Objects.requireNonNull(in);
    this.queues = new LinkedList<>();
    for (int i = 0; i < numDownStreams; i++) {
      this.queues.add(new ArrayBlockingQueue<>(queueSize));
    }
    this.streams = createDownStreams();
  }

  @SuppressWarnings("WeakerAccess")
  public List<Stream<T>> streams() {
    return Collections.unmodifiableList(this.streams);
  }

  @Override
  public void run() {
    List<Queue<Object>> pendings = new LinkedList<>();
    Stream.concat(in, Stream.of(SENTINEL))
        .forEach((Object t) -> {
          pendings.addAll(this.queues);
          synchronized (this.queues) {
            while (!pendings.isEmpty()) {
              this.queues.stream()
                  .filter(pendings::contains)
                  .filter(queue -> queue.offer(t))
                  .forEach(pendings::remove);
              this.queues.notifyAll();
              try {
                this.queues.wait();
              } catch (InterruptedException ignored) {
              }
            }
          }
        });
  }

  public static <T> Connector<T> tee(Stream<T> in) {
    return new Connector<>(in);
  }

  public static <T> Connector<T> tee(Stream<T> in, int queueSize) {
    return tee(in).setQueueSize(queueSize);
  }

  private List<Stream<T>> createDownStreams() {
    return queues.stream()
        .map((Queue<Object> queue) -> (Iterable<T>) () -> new Iterator<T>() {
              Object next;

              @Override
              public boolean hasNext() {
                if (next == null)
                  getNext();
                return next != SENTINEL;
              }

              @SuppressWarnings("unchecked")
              @Override
              public T next() {
                if (next == null)
                  getNext();
                T ret = check((T) next, v -> v != SENTINEL, NoSuchElementException::new);
                if (next != SENTINEL)
                  next = null;
                return ret;
              }

              private void getNext() {
                synchronized (queues) {
                  while ((next = pollQueue()) == null) {
                    try {
                      queues.wait();
                    } catch (InterruptedException ignored) {
                    }
                  }
                }
              }

              private Object pollQueue() {
                try {
                  return next = queue.poll();
                } finally {
                  queues.notifyAll();
                }
              }
            }
        ).map(
            (Iterable<T> iterable) -> StreamSupport.stream(iterable.spliterator(), false)
        ).collect(
            toList()
        );
  }

  private static <T, E extends Throwable> T check(T value, Predicate<T> check, Supplier<E> exceptionSupplier) throws E {
    if (check.test(value))
      return value;
    throw exceptionSupplier.get();
  }

  public static class Connector<T> {
    private final Stream<T> in;
    private       int                       queueSize   = 8192;
    private       long                      timeOut     = 60;
    private       TimeUnit                  timeOutUnit = TimeUnit.SECONDS;
    private final List<Consumer<Stream<T>>> consumers   = new LinkedList<>();


    public Connector(Stream<T> in) {
      this.in = Objects.requireNonNull(in);
    }

    public Connector<T> setQueueSize(int queueSize) {
      this.queueSize = check(queueSize, v -> v > 0, IllegalArgumentException::new);
      return this;
    }

    public Connector<T> timeOut(long timeOut, TimeUnit timeUnit) {
      this.timeOut = check(timeOut, v -> v > 0, IllegalArgumentException::new);
      this.timeOutUnit = Objects.requireNonNull(timeUnit);
      return this;
    }


    public <U> Connector<T> connect(Function<Stream<T>, Stream<U>> map, Consumer<U> action) {
      this.consumers.add(stream -> map.apply(stream).forEach(action));
      return this;
    }

    public Connector<T> connect(Consumer<T> consumer) {
      this.connect(stream -> stream, consumer);
      return this;
    }

    /**
     * Blocks until all tasks have completed execution after a
     * shutdown request, or the timeout occurs, or the current thread
     * is interrupted, whichever happens first.
     *
     * @return {@code true} if this executor terminated and
     * {@code false} if the timeout elapsed before termination
     * @throws InterruptedException if interrupted while waiting
     * @see ForkJoinPool#awaitTermination(long, TimeUnit)
     */
    public boolean run() throws InterruptedException {
      return run(this.timeOut, this.timeOutUnit);
    }

    /**
     * Blocks until all tasks have completed execution after a
     * shutdown request, or the timeout occurs, or the current thread
     * is interrupted, whichever happens first.
     *
     * @param timeOut the maximum time to wait
     * @param unit    the time unit of the timeout argument
     * @return {@code true} if this executor terminated and
     * {@code false} if the timeout elapsed before termination
     * @throws InterruptedException if interrupted while waiting
     */
    public boolean run(long timeOut, TimeUnit unit) throws InterruptedException {
      Tee<T> tee = new Tee<>(this.in, consumers.size(), this.queueSize);
      AtomicInteger i = new AtomicInteger(0);
      ForkJoinPool pool = new ForkJoinPool(consumers.size());
      tee.start();
      try {
        consumers.stream(
        ).map(
            (Consumer<Stream<T>> consumer) -> (Runnable) () -> consumer.accept(tee.streams().get(i.getAndIncrement()))
        ).map(
            pool::submit
        ).parallel(
        ).forEach(
            task -> {
            }
        );
      } finally {
        pool.shutdown();
      }
      return pool.awaitTermination(timeOut, unit);
    }
  }
}
