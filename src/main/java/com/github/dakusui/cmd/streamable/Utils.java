package com.github.dakusui.cmd.streamable;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public enum Utils {
  ;

  public static Stream<String> drain(Stream<String> stream, File sink) {
    return stream;
  }

  public static Stream<String> drain(Stream<String> stream, Consumer<String> sink, Supplier<Stream<String>> source) {
    new Thread(() -> stream.forEach(sink)).start();
    return source.get();
  }

  public static Stream<String> openForRead(File file, Charset charset) {
    try {
      return toStream(new FileInputStream(file), charset);
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

  private enum State {
    READ,
    NOT_READ,
    END;
  }

  public static Iterator<String> toIterator(InputStream is, Charset charset) {
    return new Iterator<String>() {
      BufferedReader reader = new BufferedReader(
          new InputStreamReader(
              is,
              charset
          )
      );
      State state = State.NOT_READ;
      String next;

      @Override
      public boolean hasNext() {
        readIfNotReadYet();
        return state != State.END;
      }

      @Override
      public String next() {
        if (state == State.END)
          throw new NoSuchElementException();
        readIfNotReadYet();
        try {
          return next;
        } finally {
          state = State.NOT_READ;
        }
      }

      private void readIfNotReadYet() {
        if (state == State.NOT_READ) {
          this.next = readLine(reader);
          state = this.next == null ?
              State.END :
              State.READ;
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

  public static Consumer<String> openForWrite(File file, Charset charset) {
    try {
      return toConsumer(new FileOutputStream(file), charset);
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public static Consumer<String> toConsumer(OutputStream os, Charset charset) {
    PrintStream ps = new PrintStream(os);
    return ps::println;
  }
}
