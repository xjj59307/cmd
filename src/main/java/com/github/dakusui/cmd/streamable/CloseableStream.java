package com.github.dakusui.cmd.streamable;

import com.sun.xml.internal.ws.Closeable;

import java.util.stream.Stream;

public interface CloseableStream<T> extends Stream<T>, Closeable {
}
