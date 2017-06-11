# cmd

A library for Java 8 to run a shell command using a stream easily on Unix platforms. 

Creating a program that executes a shell command from Java is a tedious task but 
there are a lot of pitfalls.

This library does it on behalf of you. To run '''echo hello''', you can simply do

```java
    Cmd.cmd(Shell.local(), "echo hello").stream().forEach(System.out::println);
```

To do it over ```ssh```, do

```java
    Cmd.cmd(Shell.ssh("yourName", "yourHost"), "echo hello").stream().forEach(System.out::println);
```

If you want to specity an identity file (ssh key), you can do

```java
    Cmd.cmd(Shell.ssh("yourName", "yourHost", "/home/yourName/.ssh/id_rsa"), "echo hello").stream().forEach(System.out::println);
```

Enjoy.

# Installation

**cmd** requires Java SE8 or later. Following is a maven coordinate for it. 

```xml

  <dependency>
    <groupId>com.github.dakusui</groupId>
    <artifactId>cmdstreamer</artifactId>
    <version>[0.9.0,)</version>
  </dependency>
```

# More examples

## Redirection
```java

  public void pipe() {
    Cmd.cmd(
        Shell.local(),
        "echo hello && echo world"
    ).connect(
        "cat -n"
    ).connect(
        "sort -r"
    ).connect(
        "sed 's/hello/HELLO/'"
    ).connect(
        "sed -E 's/^ +//'"
    ).stream(
    ).map(
        s -> String.format("<%s>", s)
    ).forEach(
        System.out::println
    );
  }
```

## Tee
```java

    boolean result = Cmd.cmd(
        Shell.local(),
        "seq 1 10000"
    ).tee(
    ).connect(
        in -> Cmd.cmd(
            Shell.local(),
            "cat -n",
            in
        ).stream(
        ),
        s -> System.out.println("LEFT:" + s)
    ).connect(
        in -> Cmd.cmd(
            Shell.local(),
            "cat -n",
            in
        ).stream(),
        s -> System.out.println("RIGHT:" + s)
    ).run();

```

## Compatibility with ```commandrunner``` library
By using ```CommandUtils```, you can replace your dependency on ```commandrunner```[[0]] library.

```java


  public void runLocal_echo_hello() throws Exception {
    CommandResult result = CommandUtils.runLocal("echo hello");
    ...
  }

```

# Future works
* Implement a timeout feature

# References
* [0] "commandrunner"

[0]: https://github.com/xjj59307/commandrunner


