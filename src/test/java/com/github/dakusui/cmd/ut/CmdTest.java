package com.github.dakusui.cmd.ut;

import com.github.dakusui.cmd.Cmd;
import com.github.dakusui.cmd.StreamableQueue;
import com.github.dakusui.cmd.exceptions.CommandExecutionException;
import com.github.dakusui.cmd.exceptions.UnexpectedExitValueException;
import com.github.dakusui.cmd.utils.TestUtils;
import org.junit.Test;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.github.dakusui.cmd.Cmd.cat;
import static com.github.dakusui.cmd.Cmd.cmd;
import static com.github.dakusui.cmd.Shell.ssh;
import static com.github.dakusui.cmd.utils.TestUtils.allOf;
import static com.github.dakusui.cmd.utils.TestUtils.matcherBuilder;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class CmdTest extends TestUtils.TestBase {
  private List<String> out = Collections.synchronizedList(new LinkedList<>());

  @Test(timeout = 3_000)
  public void teeExample1() {
    cmd(
        "cat"
    ).readFrom(
        () -> Stream.of("Hello", "World")
    ).connect(
        cat().pipeline(stream -> stream.map(String::toUpperCase))
    ).connect(
        cat().pipeline(stream -> stream.map(String::toLowerCase))
    ).stream(
    ).peek(
        System.out::println
    ).forEach(
        out::add
    );

    assertThat(
        out.stream().sorted().collect(toList()),
        allOf(
            TestUtils.<List<String>, Integer>matcherBuilder(
                "size", List::size
            ).check(
                "==4", size -> size == 4
            ).build(),
            TestUtils.<List<String>, String>matcherBuilder(
                "elementAt0", o -> o.get(0)
            ).check(
                "=='HELLO'", s -> s.equals("HELLO")
            ).build(),
            TestUtils.<List<String>, String>matcherBuilder(
                "elementAt1", o -> o.get(1)
            ).check(
                "=='WORLD'", s -> s.equals("WORLD")
            ).build(),
            TestUtils.<List<String>, String>matcherBuilder(
                "elementAt2", o -> o.get(2)
            ).check(
                "=='hello'", s -> s.equals("hello")
            ).build(),
            TestUtils.<List<String>, String>matcherBuilder(
                "elementAt3", o -> o.get(3)
            ).check(
                "=='world'", s -> s.equals("world")
            ).build()
        )
    );
  }

  @Test(timeout = 3_000)
  public void streamExampleA() {
    Cmd cmd = cmd(
        "cat -n"
    );
    ((StreamableQueue<String>) cmd.stdin()).accept("Hello");
    ((StreamableQueue<String>) cmd.stdin()).accept("World");
    ((StreamableQueue<String>) cmd.stdin()).accept(null);

    cmd.stream(
    ).peek(
        System.out::println
    ).forEach(
        out::add
    );
    assertThat(
        out,
        allOf(
            TestUtils.<List<String>, Integer>matcherBuilder(
                "size", List::size
            ).check(
                "==2", size -> size == 2
            ).build(),
            TestUtils.<List<String>, String>matcherBuilder(
                "elementAt0", o -> o.get(0)
            ).check(
                "==contains'1\tHello'", s -> s.contains("1\tHello")
            ).build(),
            TestUtils.<List<String>, String>matcherBuilder(
                "elementAt1", o -> o.get(1)
            ).check(
                "==contains'2\tHello'", s -> s.contains("2\tWorld")
            ).build()
        )
    );
  }

  @Test(timeout = 3_000)
  public void streamExample0() {
    cmd(
        "echo hello"
    ).stream(
    ).peek(
        System.out::println
    ).forEach(
        out::add
    );

    assertThat(
        out,
        allOf(
            TestUtils.<List<String>, Integer>matcherBuilder(
                "size", List::size
            ).check(
                "==1", size -> size == 1
            ).build(),
            TestUtils.<List<String>, String>matcherBuilder(
                "elementAt0", o -> o.get(0)
            ).check(
                "=='hello'", "hello"::equals
            ).build()
        )
    );
  }

  @Test(timeout = 3_000)
  public void streamExample1() {
    cmd(
        "echo hello"
    ).readFrom(
        Stream::empty
    ).stream(
    ).peek(
        System.out::println
    ).forEach(
        out::add
    );

    assertThat(
        out,
        allOf(
            TestUtils.<List<String>, Integer>matcherBuilder(
                "size", List::size
            ).check(
                "==1", size -> size == 1
            ).build(),
            TestUtils.<List<String>, String>matcherBuilder(
                "elementAt0", o -> o.get(0)
            ).check(
                "=='hello'", "hello"::equals
            ).build()
        )
    );
  }

  @Test(timeout = 3_000)
  public void given100ConcatenatedEchoCommands$whenRun$thenNotBlockedAnd100LinesWritten() {
    cmd(
        IntStream.range(0, 100).mapToObj(i -> String.format("echo %d", i)).collect(Collectors.joining(" && "))
    ).readFrom(
        Stream::empty
    ).stream(
    ).peek(
        System.out::println
    ).forEach(
        out::add
    );

    assertThat(
        out,
        allOf(
            TestUtils.<List<String>, Integer>matcherBuilder(
                "size", List::size
            ).check(
                "==100", size -> size == 100
            ).build(),
            TestUtils.<List<String>, String>matcherBuilder(
                "elementAt0", o -> o.get(0)
            ).check(
                "=='0'", "0"::equals
            ).build(),
            TestUtils.<List<String>, String>matcherBuilder(
                "elementAt99", o -> o.get(99)
            ).check(
                "=='99'", "99"::equals
            ).build()
        )
    );
  }

  @Test(timeout = 3_000)
  public void streamExample2() {
    cmd(
        "cat -n"
    ).readFrom(
        () -> Stream.of("Hello", "world")
    ).stream(
    ).peek(
        System.out::println
    ).forEach(
        out::add
    );

    assertThat(
        out,
        allOf(
            TestUtils.<List<String>, Integer>matcherBuilder(
                "size", List::size
            ).check(
                "==2", size -> size == 2
            ).build(),
            TestUtils.<List<String>, String>matcherBuilder(
                "elementAt0", o -> o.get(0)
            ).check(
                "contains'1\tHello'", s -> s.contains("1\tHello")
            ).build(),
            TestUtils.<List<String>, String>matcherBuilder(
                "elementAt1", o -> o.get(1)
            ).check(
                "contains'2\tworld'", s -> s.contains("2\tworld")
            ).build()
        )
    );
  }

  @Test(timeout = 3_000)
  public void streamExample3() {
    cmd(
        "echo Hello && echo world"
    ).readFrom(
        Stream::empty
    ).connect(cmd(
        "cat -n"
    )).stream(
    ).peek(
        System.out::println
    ).forEach(
        out::add
    );

    assertThat(
        out,
        allOf(
            TestUtils.<List<String>, Integer>matcherBuilder(
                "size", List::size
            ).check(
                "==2", size -> size == 2
            ).build(),
            TestUtils.<List<String>, String>matcherBuilder(
                "elementAt0", o -> o.get(0)
            ).check(
                "contains'1\tHello'", s -> s.contains("1\tHello")
            ).build(),
            TestUtils.<List<String>, String>matcherBuilder(
                "elementAt1", o -> o.get(1)
            ).check(
                "contains'2\tworld'", s -> s.contains("2\tworld")
            ).build()
        )
    );

  }

  @Test(timeout = 3_000)
  public void streamExample4() {
    cmd(
        "echo Hello && echo world"
    ).readFrom(
        Stream::empty
    ).connect(
        cmd(
            "cat -n"
        )
    ).connect(
        cmd(
            "cat -n"
        )
    ).stream(
    ).peek(
        System.out::println
    ).forEach(
        out::add
    );

    assertThat(
        out.stream().sorted().collect(toList()),
        allOf(
            TestUtils.<List<String>, Integer>matcherBuilder(
                "size", List::size
            ).check(
                "==4", size -> size == 4
            ).build(),
            TestUtils.<List<String>, String>matcherBuilder(
                "elementAt0", o -> o.get(0)
            ).check(
                "contains'1\tHello'", s -> s.contains("1\tHello")
            ).build(),
            TestUtils.<List<String>, String>matcherBuilder(
                "elementAt1", o -> o.get(1)
            ).check(
                "contains'1\tHello'", s -> s.contains("1\tHello")
            ).build(),
            TestUtils.<List<String>, String>matcherBuilder(
                "elementAt2", o -> o.get(2)
            ).check(
                "contains'2\tworld'", s -> s.contains("2\tworld")
            ).build(),
            TestUtils.<List<String>, String>matcherBuilder(
                "elementAt3", o -> o.get(3)
            ).check(
                "contains'2\tworld'", s -> s.contains("2\tworld")
            ).build()
        )
    );
  }

  @Test(timeout = 5_000)
  public void streamExample5() {
    cmd(
        "echo world && echo Hello"
    ).connect(
        cmd(
            "sort"
        ).connect(cmd(
            "cat -n"
        ))
    ).readFrom(
        Stream::empty
    ).stream(
    ).peek(
        System.out::println
    ).forEach(
        out::add
    );

    assertThat(
        out.stream().sorted().collect(toList()),
        allOf(
            TestUtils.<List<String>, Integer>matcherBuilder(
                "size", List::size
            ).check(
                "==2", size -> size == 2
            ).build(),
            TestUtils.<List<String>, String>matcherBuilder(
                "elementAt0", o -> o.get(0)
            ).check(
                "contains'1\tHello'", s -> s.contains("1\tHello")
            ).build(),
            TestUtils.<List<String>, String>matcherBuilder(
                "elementAt1", o -> o.get(1)
            ).check(
                "contains'2\tworld'", s -> s.contains("2\tworld")
            ).build()
        )
    );
  }

  @Test(timeout = 5_000)
  public void streamExample6() {
    cmd(
        "echo world && echo Hello"
    ).readFrom(
        Stream::empty
    ).connect(
        cmd(
            "cat"
        )
    ).connect(
        cmd(
            "sort"
        ).connect(cmd(
            "cat -n"
        ))
    ).stream(
    ).peek(
        System.out::println
    ).forEach(
        out::add
    );

    assertThat(
        out.stream().sorted().collect(toList()),
        allOf(
            TestUtils.<List<String>, Integer>matcherBuilder(
                "size", List::size
            ).check(
                "==4", size -> size == 4
            ).build(),
            TestUtils.<List<String>, String>matcherBuilder(
                "elementAt0", o -> o.get(0)
            ).check(
                "contains'1\tHello'", s -> s.contains("1\tHello")
            ).build(),
            TestUtils.<List<String>, String>matcherBuilder(
                "elementAt1", o -> o.get(1)
            ).check(
                "contains'2\tworld'", s -> s.contains("2\tworld")
            ).build(),
            TestUtils.<List<String>, String>matcherBuilder(
                "elementAt2", o -> o.get(2)
            ).check(
                "contains'Hello'", s -> s.contains("Hello")
            ).build(),
            TestUtils.<List<String>, String>matcherBuilder(
                "elementAt3", o -> o.get(3)
            ).check(
                "contains'world'", s -> s.contains("world")
            ).build()
        )
    );
  }

  @Test(timeout = 3_000, expected = CommandExecutionException.class)
  public void failingStreamExample1() {
    cmd(
        "unknownCommand hello"
    ).stream(
    ).peek(
        System.out::println
    ).forEach(
        out::add
    );

    assertThat(
        out.stream().sorted().collect(toList()),
        allOf(
            matcherBuilder(
                "size", (Function<List<String>, Integer>) List::size
            ).check(
                "==2", size -> size == 2
            ).build(),
            matcherBuilder(
                "elementAt0", (List<String> o) -> o.get(0)
            ).check(
                "contains'1\tHello'", s -> s.contains("1\tHello")
            ).build(),
            matcherBuilder(
                "elementAt1", (List<String> o) -> o.get(1)
            ).check(
                "contains'2\tworld'", s -> s.contains("2\tworld")
            ).build()
        )
    );
  }

  @Test(timeout = 5_000, expected = UnexpectedExitValueException.class)
  public void failingStreamExample2() {
    cmd(
        "unknownCommand hello"
    ).connect(
        cmd("cat -n")
    ).stream(
    ).peek(
        System.out::println
    ).forEach(
        out::add
    );
    System.out.println(format("Shouldn't be executed.(tid=%d)", Thread.currentThread().getId()));
  }

  @Test
  public void failingStreamExample2b() {
    for (int i = 0; i < 100; i++) {
      System.out.println("=== " + i + " ===");
      Cmd cmd = cmd(
          "unknownCommand hello"
      ).connect(
          cmd("cat -n")
      );
      if (!TestUtils.terminatesIn(
          () -> cmd.stream(
          ).forEach(
              System.out::println
          ),
          2_000
      )) {
        ((Cmd.Impl) cmd).dump();
        throw new RuntimeException();
      }
    }
  }

  @Test(timeout = 3_000, expected = CommandExecutionException.class)
  public void failingStreamExample3() {
    cmd(
        "echo hello"
    ).connect(
        cmd("unknownCommand -n")
    ).stream(
    ).peek(
        System.out::println
    ).forEach(
        out::add
    );
  }

  @Test(timeout = 3_000, expected = CommandExecutionException.class)
  public void failingStreamExample4() {
    cmd(
        "echo hello"
    ).connect(
        cmd("cat -n")
    ).connect(
        cmd("unknownCommand -n")
    ).stream(
    ).peek(
        System.out::println
    ).forEach(
        out::add
    );
  }

  @Test(timeout = 3_000, expected = CommandExecutionException.class)
  public void failingStreamExample5() {
    cmd(
        "echo hello"
    ).connect(
        cmd("unknownCommand -n").connect(
            cmd("cat -n")
        )
    ).stream(
    ).peek(
        System.out::println
    ).forEach(
        out::add
    );
  }

  @Test(timeout = 10_000)
  public void sshExample() {
    assertEquals(
        "hello",
        cmd(
            ssh(TestUtils.userName(), TestUtils.hostName(), TestUtils.identity()),
            "echo hello"
        ).stream(
        ).collect(
            Collectors.joining()
        )
    );
  }
}
