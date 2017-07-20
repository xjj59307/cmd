package com.github.dakusui.cmd.tmp;

import com.github.dakusui.cmd.Shell;
import com.github.dakusui.cmd.core.Tee;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

public class CompatCmdTee {
  private final Tee.Connector<String> teeConnector;
  private final CompatCmd             upstream;

  public CompatCmdTee(CompatCmd upstream, Tee.Connector<String> teeConnector) {
    this.upstream = Objects.requireNonNull(upstream);
    this.teeConnector = Objects.requireNonNull(teeConnector);
  }

  public CompatCmdTee connect(Function<Stream<String>, CompatCmd> factory, Consumer<String> consumer) {
    Objects.requireNonNull(factory);
    Objects.requireNonNull(consumer);
    this.teeConnector.connect(
        in -> {
          CompatCmd cmd = factory.apply(in);
          CompatCmdTee.this.upstream.addObserver(cmd);
          return cmd.stream();
        },
        consumer
    );
    return this;
  }

  public CompatCmdTee connect(Shell shell, String command) {
    return this.connect(in -> CompatCmd.cmd(shell, command, in), s -> {
    });
  }

  public CompatCmdTee connect(String command) {
    return this.connect(upstream.getShell(), command);
  }

  public CompatCmdTee connect(Consumer<String> consumer) {
    teeConnector.connect(consumer);
    return this;
  }

  public boolean run(long timeOut, TimeUnit unit) throws InterruptedException {
    addObserverToUpstream();
    return teeConnector.run(timeOut, unit);
  }

  public boolean run() throws InterruptedException {
    addObserverToUpstream();
    try {
      return teeConnector.run();
    } finally {
      upstream.waitFor();
    }
  }

  private void addObserverToUpstream() {
    this.upstream.addObserver((upstream, upstreamException) -> teeConnector.interrupt());
  }
}
