package com.github.dakusui.cmd;

import com.github.dakusui.cmd.core.IoUtils;
import com.github.dakusui.cmd.exceptions.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class StreamableQueue<E> implements Consumer<E>, Supplier<Stream<E>> {
  private static final Logger  LOGGER = LoggerFactory.getLogger(StreamableQueue.class);
  private              boolean closed = false;
  private final BlockingQueue<Object> queue;

  public StreamableQueue(int queueSize) {
    queue = new ArrayBlockingQueue<>(queueSize);
  }

  @Override
  public Stream<E> get() {
    return StreamSupport.stream(
        ((Iterable<E>) () -> new Iterator<E>() {
          Object next = null;

          @Override
          public synchronized boolean hasNext() {
            readNextIfNotYet();
            return next != IoUtils.SENTINEL;
          }

          @Override
          public synchronized E next() {
            readNextIfNotYet();
            if (next == IoUtils.SENTINEL)
              throw new NoSuchElementException();
            try {
              //noinspection unchecked
              return (E) next;
            } finally {
              next = null;
            }
          }

          private void readNextIfNotYet() {
            if (next != null)
              return;
            synchronized (queue) {
              while (queue.peek() == null)
                try {
                  queue.wait();
                } catch (InterruptedException ignored) {
                }
              next = queue.remove();
              queue.notifyAll();
            }
          }
        }).spliterator(),
        false
    );
  }

  @Override
  public synchronized void accept(E e) {
    synchronized (queue) {
      if (closed)
        //noinspection ConstantConditions
        throw Exceptions.illegalState(String.format("closed==%s", closed), "closed==false");
      if (e == null) {
        close();
        return;
      }
      offer(e);
    }
  }

  private void offer(Object e) {
    LOGGER.trace("e={}", e);
    while (!queue.offer(e)) {
      try {
        queue.wait();
      } catch (InterruptedException ignored) {
      }
    }
    queue.notifyAll();
  }

  private void close() {
    LOGGER.debug("BEGIN:{}", this);
    synchronized (queue) {
      if (closed)
        return;
      offer(IoUtils.SENTINEL);
      closed = true;
    }
    LOGGER.debug("END:{}", this);
  }
}
