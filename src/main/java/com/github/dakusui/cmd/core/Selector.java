package com.github.dakusui.cmd.core;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

public interface Selector<T> {
  Stream<T> stream();


  @SafeVarargs
  static <T> Stream<T> select(Stream<T>... streams) {
    return select(Arrays.asList(streams));
  }

  /**
   *
   */
  static <T> Stream<T> select(List<Stream<T>> streams) {
    return new Selector.Builder<T>("Selector.select") {{
      streams.forEach(each -> add(each, IoUtils.nop(), true));
    }}.build().stream();
  }

  class Builder<T> {
    static class Record<T> {
      private final T           data;
      private final Consumer<T> consumer;
      private final boolean     passToDownstream;

      Record(T data, Consumer<T> consumer, boolean passToDownstream) {
        this.data = data;
        this.consumer = consumer;
        this.passToDownstream = passToDownstream;
      }
    }

    private final String name;

    private final Map<Stream<T>, Consumer<T>> consumers;
    private final Map<Stream<T>, Boolean>     toBePassed;


    public Builder(String name) {
      this.name = name;
      this.consumers = new LinkedHashMap<>();
      this.toBePassed = new LinkedHashMap<>();
    }

    /**
     * If {@code false} is given to {@code passToDownStream}, data from {@code stream}
     * will not be found in the stream returned by {@code Selector#stream} method.
     *
     * @param stream           A stream from which data should be read.
     * @param consumer         A consumer that consumes data from {@cdoe stream}.
     * @param passToDownStream Specified if data {@code stream} should be passed
     *                         to selector's output.
     * @return This object.
     */
    public Builder<T> add(Stream<T> stream, Consumer<T> consumer, boolean passToDownStream) {
      this.consumers.put(requireNonNull(stream), requireNonNull(consumer));
      this.toBePassed.put(stream, passToDownStream);
      return this;
    }

    public Selector<T> build() {
      return new Selector<T>() {
        @Override
        public Stream<T> stream() {
          return consumers.keySet(
          ).stream(
          ).parallel(
          ).flatMap(
              stream -> stream.map(
                  t -> new Record<>(t, consumers.get(stream), toBePassed.get(stream))
              )
          ).peek(
              r -> r
                  .consumer
                  .accept(r.data)
          ).filter(
              r -> r.passToDownstream
          ).map(
              tRecord -> tRecord.data
          ).onClose(
              () -> {
                consumers.keySet().forEach(Stream::close);
              }
          );
        }

        @Override
        public String toString() {
          return name;
        }
      };
    }
  }
}
