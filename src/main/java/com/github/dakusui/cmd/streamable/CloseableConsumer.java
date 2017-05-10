package com.github.dakusui.cmd.streamable;

import java.io.Closeable;
import java.util.function.Consumer;

public interface CloseableConsumer<T> extends Closeable, Consumer<T> {
}
